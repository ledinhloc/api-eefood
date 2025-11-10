package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.model.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostSearchService {

  private final ElasticsearchClient client;

  public List<Long> searchPostIds(
    String keyword,
    String region,
    String difficulty,
    String category,
    Integer maxCookTime,
    UserResponse user,
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

      return client.search(s -> {
          var search = s
            .index("posts")
            .from((page - 1) * size)
            .size(size);

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
//                  .field("cookTime")      // đây sẽ compile OK
//                  .lte(JsonData.of(maxCookTime))
//                ));
//              }

              return b;
            }))
              .functions(fList -> {
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
                  )).weight(1.0);
                }

                /** ---- DỊ ỨNG (giảm điểm) ---- **/
//                for (String al : allergies) {
//                  fList.filter(f -> f.match(m -> m
//                    .field("recipeIngredientKeywords")
//                    .query(al)
//                    .fuzziness("AUTO")
//                  )).weight(0.2);
//                }
                return fList;
              })
              .boostMode(FunctionBoostMode.Replace)
          ));

          // --- Sort theo score ---
          search.sort(srt -> srt.score(o -> o.order(SortOrder.Desc)));

          return search;
        }, PostDocument.class)
        .hits()
        .hits()
        .stream()
        .map(Hit::source)
        .map(PostDocument::getId)
        .toList();

    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }
}
