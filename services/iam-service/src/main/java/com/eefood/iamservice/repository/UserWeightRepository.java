package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.UserWeight;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWeightRepository extends JpaRepository<UserWeight, Long> {
  List<UserWeight> findAllByUser_IdOrderByRecordedAtDesc(Long userId);

  Optional<UserWeight> findByIdAndUser_Id(Long id, Long userId);
}
