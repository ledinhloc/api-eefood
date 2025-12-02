package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.model.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {
  List<PostViewLog> findAllByUserIdAndViewedAtAfter(Long userId, LocalDateTime since);
}
