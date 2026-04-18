package com.eefood.iamservice.repository;

import com.eefood.iamservice.model.UserWeight;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWeightRepository extends JpaRepository<UserWeight, Long> {
}
