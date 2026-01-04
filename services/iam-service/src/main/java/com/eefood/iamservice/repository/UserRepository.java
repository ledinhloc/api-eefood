package com.eefood.iamservice.repository;

import com.eefood.iamservice.enums.Role;
import com.eefood.iamservice.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  Optional<User> findByEmail(String email);

  Optional<User> findByAuthIdAndIsDeletedFalse(String authId);

  Optional<User> findByEmailAndIsDeletedFalse(String email);

  Optional<User> findByIdAndIsDeletedFalse(Long id);

  List<User> findAllByIsDeletedFalse();
  List<User> findByIdInAndIsDeletedFalse(List<Long> ids);
  List<User> findAllByRoleAndIsDeletedFalse(Role role);
  List<User> findByIsDeletedFalseAndCreatedAtAfter(LocalDateTime fromDate);

  @Query(
          value = """
        SELECT 
            address ->> 'city' AS city,
            COUNT(*) AS total
        FROM users
        WHERE address IS NOT NULL
          AND jsonb_extract_path_text(address, 'city') IS NOT NULL
        GROUP BY address ->> 'city'
        ORDER BY total DESC
    """,
          nativeQuery = true
  )
  List<Object[]> countUsersGroupByCity();
}
