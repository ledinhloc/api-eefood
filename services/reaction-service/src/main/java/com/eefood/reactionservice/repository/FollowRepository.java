package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Follow> findByFollowingId(Long userId, Pageable pageable);

    Page<Follow> findByFollowerId(Long userId, Pageable pageable);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);
}
