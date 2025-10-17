package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.CollectionPost;
import com.eefood.reactionservice.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionPostRepository extends JpaRepository<CollectionPost, Long> {
  List<CollectionPost> findAllByCollectionId(Long collectionId);
  boolean existsByCollectionIdAndPostId(Long collectionId, Long postId);
  void deleteByCollectionIdAndPostId(Long collectionId, Long postId);

  @Query(
"""
    SELECT cp.post FROM CollectionPost cp
    WHERE cp.collection.id=:collectionId
    ORDER BY cp.createdAt DESC
    LIMIT 1
""")
  Optional<Post> findLastPostByCollectionId(Long collectionId);
}
