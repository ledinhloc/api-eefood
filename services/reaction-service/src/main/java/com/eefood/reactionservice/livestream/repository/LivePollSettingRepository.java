package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LivePollSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivePollSettingRepository extends JpaRepository<LivePollSetting, Long> {
  Optional<LivePollSetting> findByPollId(Long id);
}
