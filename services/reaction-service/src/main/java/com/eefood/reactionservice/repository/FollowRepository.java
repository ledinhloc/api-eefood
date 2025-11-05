package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findByFollowingId(Long userId);

    List<Follow> findByFollowerId(Long userId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);
}
