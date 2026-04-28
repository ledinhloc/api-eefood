package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ReviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewQuestionRepository extends JpaRepository<ReviewQuestion, Long> {
    List<ReviewQuestion> findAllByIsDeletedIsFalseAndIsActiveTrue();
}
