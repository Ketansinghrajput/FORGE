package com.forge.platform.repository;

import com.forge.platform.entity.WalletTransaction;
import com.forge.platform.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // All transactions for a wallet — paginated
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    // Filter by type (CREDIT / DEBIT)
    Page<WalletTransaction> findByWalletIdAndTypeOrderByCreatedAtDesc(Long walletId, TransactionType type, Pageable pageable);
}