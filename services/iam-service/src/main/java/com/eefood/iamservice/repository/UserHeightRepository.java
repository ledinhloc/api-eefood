package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.UserHeight;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHeightRepository extends JpaRepository<UserHeight, Long> {
  List<UserHeight> findAllByUser_IdOrderByRecordedAtDesc(Long userId);

  Optional<UserHeight> findByIdAndUser_Id(Long id, Long userId);
}
