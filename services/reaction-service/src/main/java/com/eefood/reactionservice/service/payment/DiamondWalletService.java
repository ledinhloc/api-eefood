package com.eefood.reactionservice.service.payment;

import com.eefood.reactionservice.dto.response.WalletHistoryResponse;
import com.eefood.reactionservice.dto.response.payment.DiamondPackageResponse;
import com.eefood.reactionservice.enums.WalletHistoryType;
import com.eefood.reactionservice.model.payment.UserWallet;
import com.eefood.reactionservice.model.payment.WalletHistory;
import com.eefood.reactionservice.repository.DiamondPackageRepository;
import com.eefood.reactionservice.repository.payment.UserWalletRepository;
import com.eefood.reactionservice.repository.payment.WalletHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiamondWalletService {
    private final UserWalletRepository userWalletRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DiamondPackageRepository diamondPackageRepository;

    @Transactional(readOnly = true)
    public Page<WalletHistoryResponse> getWalletHistory(
            Long userId, String type, String sort, int page, int size) {

        Sort sortOrder = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page-1, size, sortOrder);

        Page<WalletHistory> historyPage;

        if (type == null || type.isBlank()) {
            historyPage = walletHistoryRepository.findByUserId(userId, pageable);
        } else {
            try {
                WalletHistoryType walletType = WalletHistoryType.valueOf(type.toUpperCase());
                historyPage = walletHistoryRepository.findByUserIdAndType(userId, walletType, pageable);
            } catch (IllegalArgumentException e) {
                return Page.empty(pageable);
            }
        }

        return historyPage.map(w -> WalletHistoryResponse.builder()
                .id(w.getId())
                .userId(w.getUserId())
                .transactionId(w.getTransactionId())
                .type(w.getType())
                .amount(w.getAmount())
                .balanceBefore(w.getBalanceBefore())
                .balanceAfter(w.getBalanceAfter())
                .createdAt(w.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public List<DiamondPackageResponse> getAllPackages() {

        return diamondPackageRepository
                .findByIsActiveTrueOrderByPriceAsc()
                .stream()
                .map(pkg -> DiamondPackageResponse.builder()
                        .id(pkg.getId())
                        .diamondAmount(pkg.getDiamondAmount())
                        .bonusDiamond(pkg.getBonusDiamond())
                        .price(pkg.getPrice())
                        .currency(pkg.getCurrency())
                        .isActive(pkg.getIsActive())
                        .build())
                .toList();
    }

    // Trừ dimond
    @Transactional(propagation = Propagation.REQUIRED)
    public void spend(Long userId, Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Spend amount must be positive");
        }

        UserWallet wallet = userWalletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for userId: " + userId));

        if (wallet.getBalanceDiamond() < amount) {
            throw new RuntimeException("Insufficient diamond balance. Current: "
                    + wallet.getBalanceDiamond() + ", Required: " + amount);
        }

        long balanceBefore = wallet.getBalanceDiamond();
        long balanceAfter = balanceBefore - amount;

        wallet.setBalanceDiamond(balanceAfter);
        wallet.setTotalSpent(wallet.getTotalSpent() + amount);
        userWalletRepository.save(wallet);

        WalletHistory history = WalletHistory.builder()
                .userId(userId)
                .transactionId(null) // Tiêu diamond không có payment transaction
                .type(WalletHistoryType.SPEND)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
        walletHistoryRepository.save(history);

        pushBalanceUpdate(userId, balanceAfter);

        log.info("Spend diamond: userId={}, amount={}, balanceBefore={}, balanceAfter={}",
                userId, amount, balanceBefore, balanceAfter);
    }

    // Nạp diamond
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void topup(Long userId, Long amount, Long transactionId) {
        // Dùng pessimistic lock tránh race condition
        UserWallet wallet = userWalletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> createWallet(userId));

        long balanceBefore = wallet.getBalanceDiamond();
        long balanceAfter = balanceBefore + amount;

        wallet.setBalanceDiamond(balanceAfter);
        wallet.setTotalTopup(wallet.getTotalTopup() + amount);
        userWalletRepository.save(wallet);

        // Ghi lịch sử
        WalletHistory history = WalletHistory.builder()
                .userId(userId)
                .transactionId(transactionId)
                .type(WalletHistoryType.TOPUP)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
        walletHistoryRepository.save(history);

        pushBalanceUpdate(userId, balanceAfter);

        log.info("Topup diamond: userId={}, amount={}, balanceBefore={}, balanceAfter={}, transactionId={}",
                userId, amount, balanceBefore, balanceAfter, transactionId);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        return userWalletRepository.findByUserId(userId)
                .map(UserWallet::getBalanceDiamond)
                .orElse(0L);
    }

    private UserWallet createWallet(Long userId) {
        log.info("Creating new wallet for userId={}", userId);
        UserWallet wallet = UserWallet.builder()
                .userId(userId)
                .balanceDiamond(0L)
                .totalTopup(0L)
                .totalSpent(0L)
                .build();
        return userWalletRepository.save(wallet);
    }

    private void pushBalanceUpdate(Long userId, long newBalance) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/wallet-balance",
                    Map.of("balance", newBalance)
            );
        } catch (Exception e) {
            log.warn("Failed to push wallet balance update for userId={}", userId, e);
        }
    }
}
