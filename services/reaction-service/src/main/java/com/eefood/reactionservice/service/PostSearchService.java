package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.eefood.reactionservice.model.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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
    String sortBy
  ) {
    try {
      return client.search(s -> {
          var search = s.index("posts");

          search.query(q -> q.bool(b -> {
            // 1. Keyword (title, content, ingredient keywords)
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

            // 4. Lọc theo danh mục (Set<String>)
            if (category != null && !category.isBlank()) {
              b.filter(f -> f.term(t -> t.field("recipeCategories.keyword").value(category)));
            }

            // 5. Lọc theo thời gian nấu (fix cú pháp)
//            if (maxCookTime != null) {
//              b.filter(f -> f.range(r -> r // r is the RangeQuery.Builder
//                // The inner 'range' call (rr -> ...) is unnecessary and causes the error.
//                .field("cookTime") // Directly set the field
//                .lte(JsonData.of(maxCookTime)) // Directly set the 'less than or equal to' value
//              ));
//            }

            return b;
          }));

          // --- Sắp xếp ---
          if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
              case "popular" -> search.sort(srt -> srt.field(f -> f.field("totalShares").order(SortOrder.Desc)));
              case "toprated" -> search.sort(srt -> srt.field(f -> f.field("reactionCounts.LIKE").order(SortOrder.Desc)));
              default -> search.sort(srt -> srt.field(f -> f.field("createdAt").order(SortOrder.Desc))); // newest
            }
          }

          return search;
        }, PostDocument.class)
        .hits().hits().stream()
        .map(Hit::source)
        .map(PostDocument::getId)
        .toList();

    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }
}
