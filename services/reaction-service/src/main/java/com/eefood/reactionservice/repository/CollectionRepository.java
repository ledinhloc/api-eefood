package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
  List<Collection> findAllByUserIdAndIsDeletedFalse(Long userId);
}
