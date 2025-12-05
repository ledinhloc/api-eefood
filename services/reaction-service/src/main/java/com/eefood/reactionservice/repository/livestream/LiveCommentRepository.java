package com.eefood.reactionservice.repository.livestream;

import com.eefood.reactionservice.model.livestream.LiveComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveCommentRepository extends JpaRepository<LiveComment, Long> {
  List<LiveComment> findByLiveStreamIdOrderByCreatedAtAsc(Long liveStreamId);

  List<LiveComment> findAllByLiveStreamIdAndIsDeletedFalseOrderByCreatedAtAsc(Long liveStreamId);

  Optional<LiveComment> findByIdAndIsDeletedFalse(Long commentId);
}