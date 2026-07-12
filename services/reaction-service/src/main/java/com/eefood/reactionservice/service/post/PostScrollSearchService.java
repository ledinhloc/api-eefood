package com.eefood.reactionservice.service.post;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostScrollSearchService {
    private final ElasticsearchClient client;
    private final PostReactionService postReactionService;
    private final CommentService commentService;
    private final PostViewLogService postViewLogService;
    private static final int DEFAULT_TOP_N = 10;


    public List<Long> searchAllPostIds(
            String keyword,
            String region,
            String difficulty,
            List<String> categories,
            Integer maxCookTime,
            UserResponse user,
            List<Long> newFollowings,
            List<Long> oldFollowings,
            Integer size
    ) {
        int topN = size != null && size > 0 ? size : DEFAULT_TOP_N;
        return searchTopForUser(keyword, region, difficulty, categories, maxCookTime,
                user, newFollowings, oldFollowings, topN);
    }

    private List<Long> searchTopForUser(
            String keyword,
            String region,
            String difficulty,
            List<String> categories,
            Integer maxCookTime,
            UserResponse user,
            List<Long> newFollowings,
            List<Long> oldFollowings,
            int size
    ) {
        try {
            List<String> reactedKeywords =
                    postReactionService.getTopKeywordsFromReactedPosts(user.getId(), 5, 20);
            List<String> commentedKeywords =
                    commentService.getTopKeywordsFromCommentedPosts(user.getId(), 5, 20);
            List<String> viewedKeywords =
                    postViewLogService.getTopKeywordsFromViewedPosts(user.getId(), 10, 5, 14);

            String userCity = user.getAddress() != null && user.getAddress().get("city") != null
                    ? user.getAddress().get("city").asText()
                    : "";

            SearchResponse<PostDocument> response = client.search(s -> s
                            .index("posts")
                            .size(size)
                            .query(q -> q.functionScore(fs -> fs
                                    .query(base -> base.bool(b -> {
                                        if (keyword != null && !keyword.isBlank()) {
                                            b.should(sh -> sh.multiMatch(mm -> mm
                                                    .fields("title^8", "recipeIngredientKeywords^3", "content")
                                                    .query(keyword)
                                            ));
                                        }

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
                                                            categories.stream().map(FieldValue::of).toList()
                                                    ))
                                            ));
                                        }

                                        if (maxCookTime != null) {
                                            b.filter(f -> f.range(r -> r
                                                    .number(n -> n.field("totalTime").lte((double) maxCookTime))
                                            ));
                                        }

                                        return b;
                                    }))
                                    .functions(f -> {

                                        boostKeywordGroup(f, reactedKeywords, 4.0);
                                        boostKeywordGroup(f, commentedKeywords, 3.5);
                                        boostKeywordGroup(f, viewedKeywords, 3.0);

                                        if (!newFollowings.isEmpty()) {
                                            f.filter(fl -> fl.terms(t -> t
                                                    .field("userId")
                                                    .terms(v -> v.value(
                                                            newFollowings.stream().map(FieldValue::of).toList()
                                                    ))
                                            )).weight(8.0);
                                        }

                                        if (!oldFollowings.isEmpty()) {
                                            f.filter(fl -> fl.terms(t -> t
                                                    .field("userId")
                                                    .terms(v -> v.value(
                                                            oldFollowings.stream().map(FieldValue::of).toList()
                                                    ))
                                            )).weight(3.0);
                                        }

                                        if (!userCity.isBlank()) {
                                            f.filter(fl -> fl.match(m -> m
                                                    .field("region")
                                                    .query(userCity)
                                                    .fuzziness("AUTO")
                                            )).weight(1.5);
                                        }

                                        return f;
                                    })
                                    .scoreMode(FunctionScoreMode.Sum)
                                    .boostMode(FunctionBoostMode.Sum)
                            ))
                            .sort(srt -> srt.score(o -> o.order(SortOrder.Desc))),
                    PostDocument.class
            );

            return extractIds(response);

        } catch (IOException e) {
            log.error("User search error", e);
            return List.of();
        }
    }

    private void boostKeywordGroup(
            FunctionScore.Builder f,
            List<String> keywords,
            double weight
    ) {
        if (keywords == null || keywords.isEmpty()) return;

        f.filter(fl -> fl.multiMatch(mm -> mm
                .fields(
                        "title^3",
                        "recipeIngredientKeywords^2",
                        "recipeCategories"
                )
                .query(String.join(" ", keywords))
                .fuzziness("1")
        )).weight(weight);
    }

    private List<Long> extractIds(SearchResponse<PostDocument> response) {
        return response.hits().hits().stream()
                .map(Hit::source)
                .map(PostDocument::getId)
                .toList();
    }
}