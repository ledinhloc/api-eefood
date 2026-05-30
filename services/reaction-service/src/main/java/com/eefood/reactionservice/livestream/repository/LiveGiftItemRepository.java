package com.eefood.reactionservice.livestream.repository;

import com.eefood.reactionservice.livestream.model.LiveGiftItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface LiveGiftItemRepository extends JpaRepository<LiveGiftItem, Long> {
    Optional<LiveGiftItem> findByIdAndIsActiveTrue(Long id);
    List<LiveGiftItem> findByIsActiveTrue();
}
