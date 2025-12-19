package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Follow;


import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Follow> findByFollowingId(Long userId, Pageable pageable);

    Page<Follow> findByFollowerId(Long userId, Pageable pageable);
    List<Follow> findByFollowerId(Long userId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    @Query("""
        SELECT f.followingId, COUNT(f.followerId)
        FROM Follow f
        GROUP BY f.followingId
        ORDER BY COUNT(f.followerId) DESC
    """)
    List<Object[]> findTopUsersByFollowerCount(Pageable pageable);
}
