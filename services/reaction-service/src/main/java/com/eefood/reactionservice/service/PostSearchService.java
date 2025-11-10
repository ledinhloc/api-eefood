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
            if (params.userPrefs != null) {
              for (pref in params.userPrefs) {
                if (doc['recipeIngredientKeywords'].contains(pref)) {
                  score += 2;
                }
                if (doc['recipeCategories'].contains(pref)) {
                  score += 1.5;
                }
              }
            }
            if (params.userRegion != null &&
                doc['region.keyword'].size() > 0 &&
                doc['region.keyword'].value == params.userRegion) {
              score += 1.0;
            }
            return score;
        """)
                .params(Map.of(
                  "userPrefs", JsonData.of(user.getEatingPreferences() != null ? user.getEatingPreferences() : List.of()),
                  "userRegion", JsonData.of(region != null ? region : "")
                ))

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
