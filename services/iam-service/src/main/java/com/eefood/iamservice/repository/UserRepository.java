package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmail(String email);

  Optional<User> findByAuthIdAndIsDeletedFalse(String authId);

  Optional<User> findByEmailAndIsDeletedFalse(String email);

  Optional<User> findByIdAndIsDeletedFalse(Long id);

  List<User> findAllByIsDeletedFalse();
  List<User> findByIdInAndIsDeletedFalse(List<Long> ids);
}
