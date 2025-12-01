package com.eefood.reactionservice.repository.collection;

import com.eefood.reactionservice.model.CollectionPost;
import com.eefood.reactionservice.model.Post;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CollectionPostRepository extends JpaRepository<CollectionPost, Long> {
  @Modifying
  @Transactional
  @Query
    (
      """
          DELETE FROM CollectionPost cp 
          WHERE cp.post.id = :postId AND cp.collection.id IN :collectionIds
      """)
  void deleteByPostIdAndCollectionIdIn(
    @Param("postId") Long postId,
    @Param("collectionIds") Set<Long> collectionIds);

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

  @Query("""
    SELECT cp.collection.id 
    FROM CollectionPost cp 
    WHERE cp.post.id = :postId 
    AND cp.collection.userId = :userId
""")
  Set<Long> findCollectionIdsByPostIdAndUserId(
    @Param("postId") Long postId,
    @Param("userId") Long userId
  );

  @Query("SELECT cp FROM CollectionPost cp WHERE cp.collection.id = :collectionId ORDER BY cp.id DESC")
  List<CollectionPost> findAllByCollectionIdOrderByIdDesc(@Param("collectionId") Long collectionId);


}
