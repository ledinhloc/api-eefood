package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ReviewOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ReviewOptionRepository extends JpaRepository<ReviewOption, Long> {
    List<ReviewOption> findAllByIdInAndIsDeletedFalse(Set<Long> ids);
}
