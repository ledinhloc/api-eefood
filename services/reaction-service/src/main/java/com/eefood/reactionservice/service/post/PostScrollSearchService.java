package com.eefood.reactionservice.service.post;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.service.comment.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostScrollSearchService {
    private final ElasticsearchClient client;
    private final PostReactionService postReactionService;
    private final CommentService commentService;
    private final PostViewLogService postViewLogService;

    public List<Long> searchAllPostIds(
            String keyword,
            String region,
            String difficulty,
            List<String> categories,
            Integer maxCookTime,
            UserResponse user,
            List<Long> newFollowings,
            List<Long> oldFollowings
    ) {
        if (user == null) {
            return scrollSearchForGuest(keyword, region, difficulty, categories, maxCookTime);
        } else {
            return scrollSearchForUser(keyword, region, difficulty, categories, maxCookTime,
                    user, newFollowings, oldFollowings);
        }
    }

    private List<Long> scrollSearchForGuest(
            String keyword,
            String region,
            String difficulty,
            List<String> categories,
            Integer maxCookTime
    ) {
        List<Long> allIds = new ArrayList<>();

        try {
            log.info("Guest scroll search - basic query without personalization");

            SearchResponse<PostDocument> response = client.search(s -> {
                var search = s
                        .index("posts")
                        .size(1000)
                        .scroll(t -> t.time("1m"));

                search.query(q -> {
                    // Nếu KHÔNG có keyword và KHÔNG có filters → match all
                    boolean hasKeyword = keyword != null && !keyword.isBlank();
                    boolean hasFilters = (region != null && !region.isBlank()) ||
                            (difficulty != null && !difficulty.isBlank()) ||
                            (categories != null && !categories.isEmpty()) ||
                            (maxCookTime != null);

                    if (!hasKeyword && !hasFilters) {
                        // Không có gì → match all, sort by createdAt
                        log.info("No filters, returning all posts");
                        return q.matchAll(m -> m);
                    }

                    // Có keyword hoặc filters → dùng bool query
                    return q.bool(b -> {
                        // Keyword search (optional)
                        if (hasKeyword) {
                            b.should(sh -> sh.multiMatch(mm -> mm
                                    .fields("title^8", "recipeIngredientKeywords^3", "content")
                                    .query(keyword)
                            ));
                        }

                        // Filters
                        if (region != null && !region.isBlank()) {
                            b.filter(f -> f.term(t -> t.field("region.keyword").value(region)));
                        }

                        if (difficulty != null && !difficulty.isBlank()) {
                            b.filter(f -> f.term(t -> t.field("difficulty.keyword").value(difficulty)));
                        }

                        if (categories != null && !categories.isEmpty()) {
                            b.filter(f -> f.terms(t -> t
                                    .field("recipeCategories.keyword")
                                    .terms(v -> v.value(
                                            categories.stream()
                                                    .map(FieldValue::of)
                                                    .toList()
                                    ))
                            ));
                        }

                        if (maxCookTime != null) {
                            b.filter(filterQuery -> filterQuery
                                    .range(rangeQuery -> rangeQuery
                                            .number(numberQuery -> numberQuery
                                                    .field("totalTime")
                                                    .lte((double) maxCookTime)
                                            )
                                    )
                            );
                        }

                        // Nếu chỉ có filters mà không có keyword
                        // Thì phải set minimumShouldMatch = 0
                        if (!hasKeyword) {
                            b.minimumShouldMatch("0");
                        }

                        return b;
                    });
                });

                // Nếu có keyword → sort by score, không thì sort by date
                if (keyword != null && !keyword.isBlank()) {
                    search.sort(srt -> srt.score(o -> o.order(SortOrder.Desc)));
                } else {
                    // Sort by newest first
                    search.sort(srt -> srt.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                }

                return search;
            }, PostDocument.class);

            final String[] scrollId = new String[]{response.scrollId()};
            collectIds(response, allIds);

            // Scroll through all results
            while (true) {
                ScrollResponse<PostDocument> scrollResponse = client.scroll(
                        ScrollRequest.of(r -> r.scrollId(scrollId[0]).scroll(t -> t.time("1m"))),
                        PostDocument.class
                );

                if (scrollResponse.hits().hits().isEmpty()) {
                    break;
                }

                collectIds(scrollResponse, allIds);
                scrollId[0] = scrollResponse.scrollId();
            }

            client.clearScroll(c -> c.scrollId(scrollId[0]));

            log.info("Guest scroll search found {} posts", allIds.size());
            return allIds;

        } catch (IOException e) {
            log.error("Guest scroll search error", e);
            return List.of();
        }
    }

    private List<Long> scrollSearchForUser(
            String keyword,
            String region,
            String difficulty,
            List<String> categories,
            Integer maxCookTime,
            UserResponse user,
            List<Long> newFollowings,
            List<Long> oldFollowings
    ) {
        List<Long> allIds = new ArrayList<>();

        try {
            log.info("User scroll search with personalization - userId: {}", user.getId());

            final String userCity = (user.getAddress() != null && user.getAddress().get("city") != null)
                    ? user.getAddress().get("city").asText()
                    : "";

            final List<String> eatingPrefs = user.getEatingPreferences() != null
                    ? user.getEatingPreferences()
                    : List.of();

            final List<String> dietaryPrefs = user.getDietaryPreferences() != null
                    ? user.getDietaryPreferences()
                    : List.of();

            final List<String> allergies = user.getAllergies() != null
                    ? user.getAllergies()
                    : List.of();

            final List<String> reactedPostKeywords = postReactionService.getTopKeywordsFromReactedPosts(user.getId(), 5, 20);
            log.info("-----reactedPostKeywords: {}", reactedPostKeywords);

            final List<String> commentedPostKeywords = commentService.getTopKeywordsFromCommentedPosts(user.getId(), 5, 20);

            final List<String> viewedPostKeywords = postViewLogService.getTopKeywordsFromViewedPosts(user.getId(), 10, 5, 14);
            log.info("-----viewedPostKeywords: {}", viewedPostKeywords);

            SearchResponse<PostDocument> response = client.search(s -> {
                var search = s
                        .index("posts")
                        .size(1000)
                        .scroll(t -> t.time("1m"));

                // Function score với personalization
                search.query(q -> q.functionScore(fs -> fs
                                .query(base -> base.bool(b -> {
                                    // 1. Keyword
                                    if (keyword != null && !keyword.isBlank()) {
                                        b.should(sh -> sh.multiMatch(mm -> mm
                                                .fields("title^8", "recipeIngredientKeywords^3", "content")
                                                .query(keyword)
                                        ));
                                    }

                                    // 2. Lọc theo region
                                    if (region != null && !region.isBlank()) {
                                        b.filter(f -> f.term(t -> t.field("region.keyword").value(region)));
                                    }

                                    // 3. Lọc theo độ khó
                                    if (difficulty != null && !difficulty.isBlank()) {
                                        b.filter(f -> f.term(t -> t.field("difficulty.keyword").value(difficulty)));
                                    }

                                    // 4. Lọc theo categories
                                    if (categories != null && !categories.isEmpty()) {
                                        b.filter(f -> f.terms(t -> t
                                                .field("recipeCategories.keyword")
                                                .terms(v -> v.value(
                                                        categories.stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                ))
                                        ));
                                    }

                                    // 5. Lọc theo thời gian nấu
                                    if (maxCookTime != null) {
                                        b.filter(filterQuery -> filterQuery
                                                .range(rangeQuery -> rangeQuery
                                                        .number(numberQuery -> numberQuery
                                                                .field("totalTime")
                                                                .lte((double) maxCookTime)
                                                        )
                                                )
                                        );
                                    }
                                    return b;
                                }))
                                .functions(fList -> {
                                    /** ---- SỞ THÍCH ĂN UỐNG ---- **/
                                    for (String pref : eatingPrefs) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeIngredientKeywords")
                                                .query(pref)
                                                .fuzziness("AUTO")
                                        )).weight(3.0);

                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeCategories")
                                                .query(pref)
                                                .fuzziness("AUTO")
                                        )).weight(2.0);
                                    }

                                    /** ---- ĐỊA CHỈ / THÀNH PHỐ ---- **/
                                    if (!userCity.isBlank()) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("region")
                                                .query(userCity)
                                                .fuzziness("AUTO")
                                        )).weight(1.5);
                                    }

                                    /** ---- CHẾ ĐỘ ĂN ---- **/
                                    for (String diet : dietaryPrefs) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeCategories")
                                                .query(diet)
                                                .fuzziness("AUTO")
                                        )).weight(2.5);

                                        fList.filter(f -> f.match(m -> m
                                                .field("content")
                                                .query(diet)
                                                .fuzziness("AUTO")
                                        )).weight(1.5);
                                    }

                                    /** ---- DỊ ỨNG (giảm điểm) ---- **/
//                            for (String al : allergies) {
//                                fList.filter(f -> f.match(m -> m
//                                        .field("recipeIngredientKeywords")
//                                        .query(al)
//                                        .fuzziness("AUTO")
//                                )).weight((double) -5);
//                            }

                                    /** ---- ƯU TIÊN POST CỦA NGƯỜI ĐANG FOLLOW ---- **/
                                    if (!newFollowings.isEmpty()) {
                                        fList.filter(f -> f.terms(t -> t
                                                .field("userId")
                                                .terms(v -> v.value(
                                                        newFollowings.stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                ))
                                        )).weight(8.0);
                                    }

                                    if (!oldFollowings.isEmpty()) {
                                        fList.filter(f -> f.terms(t -> t
                                                .field("userId")
                                                .terms(v -> v.value(
                                                        oldFollowings.stream()
                                                                .map(FieldValue::of)
                                                                .toList()
                                                ))
                                        )).weight(3.0);
                                    }

                                    /** ---- POST TƯƠNG TỰ VỚI POST ĐÃ REACT ---- **/
                                    for (String k : reactedPostKeywords) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeIngredientKeywords")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(4.0);

                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeCategories")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(3.0);

                                        fList.filter(f -> f.match(m -> m
                                                .field("title")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(2.0);
                                    }

                                    /** ---- POST TƯƠNG TỰ VỚI POST ĐÃ COMMENT ---- **/
                                    for (String k : commentedPostKeywords) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeIngredientKeywords")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(4.0);

                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeCategories")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(3.0);

                                        fList.filter(f -> f.match(m -> m
                                                .field("title")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(2.0);
                                    }

                                    /** ---- POST TƯƠNG TỰ VỚI POST ĐÃ XEM ---- **/
                                    for (String k : viewedPostKeywords) {
                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeIngredientKeywords")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(3.5);

                                        fList.filter(f -> f.match(m -> m
                                                .field("recipeCategories")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(2.5);

                                        fList.filter(f -> f.match(m -> m
                                                .field("title")
                                                .query(k)
                                                .fuzziness("AUTO")
                                        )).weight(1.5);
                                    }

                                    // Để tránh lỗi "functions list is empty"
                                    if (eatingPrefs.isEmpty() && dietaryPrefs.isEmpty() && allergies.isEmpty() &&
                                            newFollowings.isEmpty() && oldFollowings.isEmpty() &&
                                            reactedPostKeywords.isEmpty() && commentedPostKeywords.isEmpty() &&
                                            viewedPostKeywords.isEmpty() && userCity.isBlank()) {
                                        // Thêm một function mặc định để ES không báo lỗi
                                        fList.filter(f -> f.matchAll(m -> m)).weight(1.0);
                                    }

                                    return fList;
                                })
                                .scoreMode(FunctionScoreMode.Sum)
                                .boostMode(FunctionBoostMode.Sum)
                ));

                // Sort theo score
                search.sort(srt -> srt.score(o -> o.order(SortOrder.Desc)));

                return search;
            }, PostDocument.class);

            final String[] scrollId = new String[]{response.scrollId()};
            collectIds(response, allIds);

            // Scroll through all results
            while (true) {
                ScrollResponse<PostDocument> scrollResponse = client.scroll(
                        ScrollRequest.of(r -> r.scrollId(scrollId[0]).scroll(t -> t.time("1m"))),
                        PostDocument.class
                );

                if (scrollResponse.hits().hits().isEmpty()) {
                    break;
                }

                collectIds(scrollResponse, allIds);
                scrollId[0] = scrollResponse.scrollId();
            }

            client.clearScroll(c -> c.scrollId(scrollId[0]));

            log.info("User scroll search found {} posts", allIds.size());
            return allIds;

        } catch (IOException e) {
            log.error("User scroll search error for userId: {}", user.getId(), e);
            return List.of();
        }
    }

    private void collectIds(SearchResponse<PostDocument> response, List<Long> collector) {
        response.hits().hits().stream()
                .map(Hit::source)
                .map(PostDocument::getId)
                .forEach(collector::add);
    }

    private void collectIds(ScrollResponse<PostDocument> response, List<Long> collector) {
        response.hits().hits().stream()
                .map(Hit::source)
                .map(PostDocument::getId)
                .forEach(collector::add);
    }
}