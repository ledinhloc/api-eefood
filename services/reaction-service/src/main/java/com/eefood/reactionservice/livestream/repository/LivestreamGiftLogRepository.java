package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LivestreamGiftLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LivestreamGiftLogRepository extends JpaRepository<LivestreamGiftLog, Long> {
}
