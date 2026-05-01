package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.UserWeight;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWeightRepository extends JpaRepository<UserWeight, Long> {
  List<UserWeight> findAllByUser_IdOrderByRecordedDateDesc(Long userId);

  Optional<UserWeight> findByIdAndUser_Id(Long id, Long userId);

  boolean existsByUser_IdAndRecordedDate(Long userId, LocalDate recordedDate);

  boolean existsByUser_IdAndRecordedDateAndIdNot(Long userId, LocalDate recordedDate, Long id);
}
