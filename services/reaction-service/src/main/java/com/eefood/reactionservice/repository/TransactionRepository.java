package com.eefood.reactionservice.repository;

import com.eefood.reactionservice.enums.TransactionStatus;
import com.eefood.reactionservice.model.payment.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndStatus(Long userId, TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.paymentUrl LIKE %:txnRef%")
    Optional<Transaction> findByTxnRef(@Param("txnRef") String txnRef);

    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.userId = :userId")
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
