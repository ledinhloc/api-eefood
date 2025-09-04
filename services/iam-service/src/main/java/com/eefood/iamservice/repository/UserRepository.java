package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  Optional<User> findByAuthIdAndIsDeletedFalse(String authId);
  Optional<User> findByEmailAndIsDeletedFalse(String email);
  Optional<User> findByIdAndIsDeletedFalse(Long id);
}
