package com.eefood.reactionservice.repository.livestream;

import com.eefood.reactionservice.enums.LiveStreamStatus;
import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.livestream.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

  LiveStream findTopByUserIdAndStatusInOrderByIdDesc(Long userId, List<LiveStreamStatus> scheduled);
}
