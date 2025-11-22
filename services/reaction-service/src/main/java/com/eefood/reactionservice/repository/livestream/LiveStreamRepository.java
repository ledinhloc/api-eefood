package com.eefood.reactionservice.repository.livestream;

import com.eefood.reactionservice.model.Collection;
import com.eefood.reactionservice.model.livestream.LiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

}
