package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.UserHeight;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHeightRepository extends JpaRepository<UserHeight, Long> {
}
