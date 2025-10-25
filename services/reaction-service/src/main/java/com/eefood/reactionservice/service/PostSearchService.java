package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.eefood.reactionservice.model.PostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch.core.search.Hit;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostSearchService {
  private final ElasticsearchClient client;

  public List<Long> searchPostIds(String keyword) {
    if (keyword == null || keyword.isBlank()) return List.of();

    try {
      SearchResponse<PostDocument> response = client.search(s -> s
          .index("posts")
          .query(q -> q
            .multiMatch(m -> m
              .fields("title^2", "content")
              .query(keyword)
            )
          ),
        PostDocument.class);

      return response.hits().hits().stream()
        .map(Hit::source)
        .map(PostDocument::getId)
        .collect(Collectors.toList());

    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }
}
