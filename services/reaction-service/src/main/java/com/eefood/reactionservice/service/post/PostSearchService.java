package com.eefood.reactionservice.service.post;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.service.comment.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchService {

  private final ElasticsearchClient client;
  private final PostReactionService postReactionService;
  private final CommentService commentService;
  private final PostViewLogService postViewLogService;

  public SearchResult searchPostIds(
    String keyword,
    String region,
    String difficulty,
    String category,
    Integer maxCookTime,
    UserResponse user,
    List<Long> newFollowings,
    List<Long> oldFollowings,
    int page,
    int size
  ) {
    if (user == null) {
      return searchForGuest(keyword, region, difficulty, category, maxCookTime, page, size);
    } else {
      return searchForUser(keyword, region, difficulty, category, maxCookTime, user, newFollowings, oldFollowings, page, size);
    }
  }

  private SearchResult searchForGuest(
    String keyword,
    String region,
    String difficulty,
    String category,
    Integer maxCookTime,
    int page,
    int size
  ) {
    try {
      log.info("Guest search - basic query without personalization");

      var response = client.search(s -> {
        var search = s
          .index("posts")
          .from((page - 1) * size)
          .size(size);

        log.info("-------Guest search params - page: {}, size: {}, from: {}", page, size, (page - 1) * size);

        search.query(q -> {
          // Nếu KHÔNG có keyword và KHÔNG có filters → match all
          boolean hasKeyword = keyword != null && !keyword.isBlank();
          boolean hasFilters = (region != null && !region.isBlank()) ||
            (difficulty != null && !difficulty.isBlank()) ||
            (category != null && !category.isBlank()) ||
            (maxCookTime != null);

          if (!hasKeyword && !hasFilters) {
            //  Không có gì → match all, sort by createdAt
            log.info("No filters, returning all posts");
            return q.matchAll(m -> m);
          }

          // Có keyword hoặc filters → dùng bool query
          return q.bool(b -> {
            // Keyword search (optional)
            if (hasKeyword) {
              b.should(sh -> sh.multiMatch(mm -> mm
                .fields("title^4", "recipeIngredientKeywords^2", "content")
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

            if (category != null && !category.isBlank()) {
              b.filter(f -> f.term(t -> t.field("recipeCategories.keyword").value(category)));
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

      List<Long> ids = response.hits()
        .hits()
        .stream()
        .map(Hit::source)
        .map(PostDocument::getId)
        .toList();

      long total = response.hits().total() != null
        ? response.hits().total().value()
        : 0;

      log.info("Guest search found {} posts", total);
      return new SearchResult(ids, total);

    } catch (IOException e) {
      log.error("Guest search error", e);
      return new SearchResult(List.of(), 0L);
    }
  }

  private SearchResult searchForUser(
    String keyword,
    String region,
    String difficulty,
    String category,
    Integer maxCookTime,
    UserResponse user,
    List<Long> newFollowings,
    List<Long> oldFollowings,
    int page,
    int size
  ) {
    try {
      log.info("User search with personalization - userId: {}", user.getId());

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

      var response = client.search(s -> {
        var search = s
          .index("posts")
          .from((page - 1) * size)
          .size(size);

        log.info("-------User search params - page: {}, size: {}, from: {}", page, size, (page - 1) * size);

        // Function score với personalization
        search.query(q -> q.functionScore(fs -> fs
          .query(base -> base.bool(b -> {
            // 1. Keyword
            if (keyword != null && !keyword.isBlank()) {
              b.should(sh -> sh.multiMatch(mm -> mm
                .fields("title^4", "recipeIngredientKeywords^2", "content")
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

            // 4. Lọc theo category
            if (category != null && !category.isBlank()) {
              b.filter(f -> f.term(t -> t.field("recipeCategories.keyword").value(category)));
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
            for (String al : allergies) {
              fList.filter(f -> f.match(m -> m
                .field("recipeIngredientKeywords")
                .query(al)
                .fuzziness("AUTO")
              )).weight(0.2);
            }

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

//            // Để tránh lỗi "functions list is empty"
//            if (eatingPrefs.isEmpty() && dietaryPrefs.isEmpty() && allergies.isEmpty() &&
//              newFollowings.isEmpty() && oldFollowings.isEmpty() &&
//              reactedPostKeywords.isEmpty() && commentedPostKeywords.isEmpty() &&
//              viewedPostKeywords.isEmpty() && userCity.isBlank()) {
//              // Thêm một function mặc định để ES không báo lỗi
//              fList.weight(w -> w.weight(1.0));
//            }

            return fList;
          })
          .scoreMode(FunctionScoreMode.Sum)
          .boostMode(FunctionBoostMode.Sum)
        ));

        // Sort theo score
        search.sort(srt -> srt.score(o -> o.order(SortOrder.Desc)));

        return search;
      }, PostDocument.class);

      List<Long> ids = response.hits()
        .hits()
        .stream()
        .map(Hit::source)
        .map(PostDocument::getId)
        .toList();

      long total = response.hits().total() != null
        ? response.hits().total().value()
        : 0;

      log.info("User search found {} posts", total);
      return new SearchResult(ids, total);

    } catch (IOException e) {
      log.error("User search error for userId: {}", user.getId(), e);
      return new SearchResult(List.of(), 0L);
    }
  }
}
