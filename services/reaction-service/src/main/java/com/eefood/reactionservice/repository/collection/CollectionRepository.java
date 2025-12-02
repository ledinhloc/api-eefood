package com.eefood.reactionservice.repository.collection;

import com.eefood.reactionservice.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
  List<Collection> findAllByUserIdAndIsDeletedFalse(Long userId);
  boolean existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(Long userId, String name);
  Optional<Collection> findByIdAndIsDeletedFalse(Long id);
  List<Collection> findAllByIdInAndIsDeletedFalse(Set<Long> toRemove);
  List<Collection> findAllByUserIdAndIsDeletedFalseOrderByIdDesc(Long userId);
}
