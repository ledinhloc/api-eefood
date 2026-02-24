package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.livestream.model.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

  LiveStream findTopByUserIdAndStatusInOrderByIdDesc(Long userId, List<LiveStreamStatus> scheduled);
}
