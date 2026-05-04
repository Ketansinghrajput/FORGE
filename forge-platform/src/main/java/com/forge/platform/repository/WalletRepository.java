package com.forge.platform.repository;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import jakarta.persistence.LockModeType; // 🔥 Important
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // 🔥 Important
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Normal fetch (Read-only tasks ke liye)
    Optional<Wallet> findByUser(User user);

    Optional<Wallet> findByUserEmail(String email);

    Optional<Wallet> findByUserId(Long userId);

    // 🔥 SENSEI FIX: Pessimistic Locking for Concurrency
    // Jab balance update (Top-up/Bid) karna ho, tab iska use kar
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user = :user")
    Optional<Wallet> findByUserWithLock(User user);
}