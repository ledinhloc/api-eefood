package com.eefood.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.eefood.recipeservice.model.RecipeDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeSearchService {
  private final ElasticsearchClient client;

  public List<Long> searchRecipeIds(String keyword, String region, String difficulty) {
    try {
      SearchResponse<RecipeDocument> response = client.search(s -> {
        s.index("recipes");

        // === Query logic ===
        s.query(q -> q
          .bool(b -> {
            if (keyword != null && !keyword.isBlank()) {
              b.must(m -> m
                .multiMatch(mm -> mm
                  .fields("title^2", "description")
                  .query(keyword)
                  .operator(Operator.And)
                )
              );
            }
            if (region != null && !region.isBlank()) {
              b.must(m -> m
                .match(mq -> mq
                  .field("region")
                  .query(region)
                )
              );
            }
            if (difficulty != null && !difficulty.isBlank()) {
              b.must(m -> m
                .match(mq -> mq
                  .field("difficulty")
                  .query(difficulty)
                )
              );
            }
            return b;
          })
        );
        return s;
      }, RecipeDocument.class);

      return response.hits().hits().stream()
        .map(Hit::source)
        .map(RecipeDocument::getId)
        .collect(Collectors.toList());
    } catch (IOException e) {
      log.error("Error searching recipes: {}", e.getMessage());
      return List.of();
    }
  }
}
