package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostIndexer implements CommandLineRunner {

  private final PostRepository postRepo;
  private final ElasticsearchClient client;

  @Override
  public void run(String... args) throws Exception {
    var posts = postRepo.findAll().stream()
      .filter(p -> !p.getIsDeleted())
      .map(p -> PostDocument.builder()
        .id(p.getId())
        .userId(p.getUserId())
        .title(p.getTitle())
        .content(p.getContent())
        .imageUrl(p.getImageUrl())
        .build()
      ).toList();

    for (PostDocument post : posts) {
      client.index(i -> i
        .index("posts")
        .id(String.valueOf(post.getId()))
        .document(post)
      );
    }

    log.info("Indexed {} posts to Elasticsearch", posts.size());
  }
}
