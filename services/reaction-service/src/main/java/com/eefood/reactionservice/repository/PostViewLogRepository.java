package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.PostViewLog;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {
  List<PostViewLog> findAllByUserIdAndViewedAtAfter(Long userId, LocalDateTime since);
}
