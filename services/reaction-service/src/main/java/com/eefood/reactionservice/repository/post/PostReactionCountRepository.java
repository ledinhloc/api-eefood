package com.eefood.reactionservice.repository.post;

import com.eefood.reactionservice.enums.ReactionType;
import com.eefood.reactionservice.model.PostReactionCount;
import com.eefood.reactionservice.model.PostReactionCountId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostReactionCountRepository extends JpaRepository<PostReactionCount, PostReactionCountId> {
    @Query("""
        SELECT prc.post.id,
               SUM(prc.count)
        FROM PostReactionCount prc
        WHERE prc.reactionType IN :reactionTypes
        GROUP BY prc.post.id
        ORDER BY SUM(prc.count) DESC
    """)
    List<Object[]> findTopPostsByReactionTypes(
            List<ReactionType> reactionTypes,
            Pageable pageable
    );
}
