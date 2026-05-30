package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.model.payment.DiamondPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiamondPackageRepository extends JpaRepository<DiamondPackage, Long> {
    List<DiamondPackage> findByIsActiveTrue();

    Optional<DiamondPackage> findByIdAndIsActiveTrue(Long id);
}
