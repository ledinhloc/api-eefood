package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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

      Map<String, JsonData> scriptParams = new HashMap<>();
      List<String> prefs = user.getEatingPreferences() != null ? user.getEatingPreferences() : List.of();
      scriptParams.put("userPrefs", JsonData.of(prefs));
      scriptParams.put("userRegion", JsonData.of(region != null ? region : ""));

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
            // --- SCRIPT SCORE ---
            .functions(fn -> fn.scriptScore(ss -> ss
              .script(sc -> sc
                .source("""
            double score = 1.0;
            
            // Tăng điểm theo sở thích ăn uống
            if (params.userPrefs != null && params.userPrefs.length > 0) {
                if (doc.containsKey('recipeIngredientKeywords.keyword') && doc['recipeIngredientKeywords.keyword'].size() > 0) {
                    for (pref in params.userPrefs) {
                        for (kw in doc['recipeIngredientKeywords.keyword']) {
                            if (kw == pref) {
                                score += 2;
                                break;
                            }
                        }
                    }
                }
                if (doc.containsKey('recipeCategories.keyword') && doc['recipeCategories.keyword'].size() > 0) {
                    for (pref in params.userPrefs) {
                        for (cat in doc['recipeCategories.keyword']) {
                            if (cat == pref) {
                                score += 1.5;
                                break;
                            }
                        }
                    }
                }
            }

            // Tăng điểm theo region
//            if (params.userRegion != null && params.userRegion != "" 
//                && doc.containsKey('region.keyword') && doc['region.keyword'].size() > 0
//                && doc['region.keyword'].value == params.userRegion) {
//                score += 1;
//            }

            return score;
        """)
                .params(scriptParams)
              )
            ))
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
