package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.model.ApprovePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovePostRepository extends JpaRepository<ApprovePost, Long> {

  List<ApprovePost> findByPostIdOrderByCreatedAtDesc(Long postId);
}
