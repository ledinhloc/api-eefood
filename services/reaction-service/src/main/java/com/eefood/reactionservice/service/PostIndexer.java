package com.eefood.reactionservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.eefood.reactionservice.model.Post;
import com.eefood.reactionservice.model.PostDocument;
import com.eefood.reactionservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostIndexer implements CommandLineRunner {

  private final PostRepository postRepo;
  private final ElasticsearchClient client;
  private final PostMapper postMapper;

  @Override
  public void run(String... args) throws Exception {
    indexAllPosts();
  }

  /**
   * Index toan bo post
   */
  private void indexAllPosts() throws IOException {
    List<PostDocument> posts = postRepo.findAll().stream()
      .filter(p -> !p.getIsDeleted())
      .map(postMapper::toDocument)
      .toList();

    for (PostDocument post : posts) {
      client.index(i -> i
        .index("posts")
        .id(String.valueOf(post.getId()))
        .document(post)
      );
    }
    log.info("Indexed {} posts to Elasticsearch", posts.size());
  }
  /**
   * Khi tao hoac update post
   */
  public void saveOrUpdatePost(Post post){
    try{
      PostDocument doc = postMapper.toDocument(post);
      client.index(i-> i
        .index("posts")
        .id(String.valueOf(doc.getId()))
        .document(doc)
      );
      log.info("Updated {} post to Elasticsearch", doc.getId());
    }catch (IOException e) {
      log.error("Error indexing post {}: {}", post.getId(), e.getMessage());
    }
  }

  /**
   * Xoa post khoi els khi xoa mem
   */
  public void deletePost(Long postId){
    try{
      client.delete(d -> d
        .index("posts")
        .id(String.valueOf(postId))
      );
      log.info("Deleted {} post to Elasticsearch", postId);
    }catch (IOException e) {
      log.error("Error deleting post {}: {}", postId, e.getMessage());
    }
  }
}
