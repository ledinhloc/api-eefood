package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.repository.PostReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    try {
      String userCity;
      if (user.getAddress() != null && user.getAddress().get("city") != null) {
        userCity = user.getAddress().get("city").asText();
      } else {
        userCity = "";
      }

      List<String> eatingPrefs = user.getEatingPreferences() != null ? user.getEatingPreferences() : List.of();
      List<String> dietaryPrefs = user.getDietaryPreferences() != null ? user.getDietaryPreferences() : List.of();
      List<String> allergies = user.getAllergies() != null ? user.getAllergies() : List.of();
      List<String> reactedPostKeywords = postReactionService.getTopKeywordsFromReactedPosts(user.getId(),5, 20);
      log.info("-----reactedPostKeywords: {}", reactedPostKeywords);
      List<String> commentedPostKeywords = commentService.getTopKeywordsFromCommentedPosts(user.getId(), 5, 20);

      List<String> viewedPostKeywords = postViewLogService.getTopKeywordsFromViewedPosts(user.getId(), 10, 5,14);
      log.info("-----viewedPostKeywords: {}", viewedPostKeywords);


        var response = client.search(s -> {
          var search = s
            .index("posts")
            .from((page - 1) * size)
            .size(size);
          log.info("-------Search params - page: {}, size: {}, from: {}", page, size, (page - 1) * size);

          // --- QUERY CHÍNH ---
          search.query(q -> q.functionScore(fs -> fs
            .query(base -> base.bool(b -> {
              // 1. Keyword
              if (keyword != null && !keyword.isBlank()) {
                b.should(sh -> sh.multiMatch(mm -> mm
                  .fields("title", "content", "recipeIngredientKeywords")
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
//              if (maxCookTime != null) {
//                b.filter(f -> f.range(r -> r
//                  .field("cookTime")
//                  .lte(JsonData.of(maxCookTime))
//                ));
//              }

              return b;
            }))
              .functions(fList -> {
//                fList.scriptScore(ss -> ss
//                  .script(sc -> sc
//                    .source("""
//                      // Tính ngày khác nhau
//                      long daysSinceCreation = (System.currentTimeMillis() - doc['createdAt'].value.getTime()) / (1000 * 60 * 60 * 24);
//
//                      // Boost mạnh cho bài mới (0-3 ngày)
//                      if (daysSinceCreation <= 3) {
//                        return _score * 3.0;
//                      }
//                      // Boost vừa (3-7 ngày)
//                      else if (daysSinceCreation <= 7) {
//                        return _score * 2.0;
//                      }
//                      // Bình thường (>7 ngày)
//                      else {
//                        return _score * 1.0;
//                      }
//                    """)
//                  )
//                );

                /** ---- SỞ THÍCH ĂN UỐNG ---- **/
                for (String pref : eatingPrefs) {

                  // Ingredient match
                  fList.filter(f -> f.match(m -> m
                    .field("recipeIngredientKeywords")
                    .query(pref)
                    .fuzziness("AUTO")
                  )).weight(3.0);

                  // Category match
                  fList.filter(f -> f.match(m -> m
                    .field("recipeCategories")
                    .query(pref)
                    .fuzziness("AUTO")
                  )).weight(2.0);
                }

                /** ---- ĐỊA CHỈ / THÀNH PHỐ ---- **/
                if (!userCity.isBlank()) {
                  fList.filter(f -> f.match(m -> m
                    .field("region")        // region trong PostDocument
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

//                if (!allergies.isEmpty()) {
//                  fList.scriptScore(ss -> ss
//                    .script(sc -> sc
//                      .source("""
//                        double penalty = 1.0;
//                        for (def allergy : params.allergies) {
//                          if (doc.containsKey('recipeIngredientKeywords') && !doc['recipeIngredientKeywords'].empty) {
//                            for (def keyword : doc['recipeIngredientKeywords'].values) {
//                              if (keyword == allergy) {
//                                penalty *= 0.3;
//                              }
//                            }
//                          }
//                        }
//                        return _score * penalty;
//                                """)
//                      .params(Map.of("allergies", JsonData.of(allergies)))
//                    )
//                  );
//                }

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

                // --- các post tương tự với post user đã react ---
                for (String k : reactedPostKeywords) {
                  // Ingredient match
                  fList.filter(f -> f.match(m -> m
                    .field("recipeIngredientKeywords")
                    .query(k)
                    .fuzziness("AUTO")
                  )).weight(4.0);

                  // Category match
                  fList.filter(f -> f.match(m -> m
                    .field("recipeCategories")
                    .query(k)
                    .fuzziness("AUTO")
                  )).weight(3.0);

                  // Title match
                  fList.filter(f -> f.match(m -> m
                    .field("title")
                    .query(k)
                    .fuzziness("AUTO")
                  )).weight(2.0);
                }

                // Lấy keyword từ comment
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

                /** ---- các post tương tự với post user đã xem chi tiết lâu ---- **/
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

                return fList;
              })
            // --- Boost / Score Mode ---
            .scoreMode(FunctionScoreMode.Sum)
              .boostMode(FunctionBoostMode.Sum)
//            .scoreMode(FunctionScoreMode.Avg)
//            .boostMode(FunctionBoostMode.Multiply)
          ))
          ;

          // --- Sort theo score ---
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

        return new SearchResult(ids, total);

    } catch (IOException e) {
      e.printStackTrace();
      return new SearchResult(List.of(), 0L);
    }
  }
}
