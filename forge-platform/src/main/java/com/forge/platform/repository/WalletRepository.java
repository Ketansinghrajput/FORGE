package com.forge.platform.repository;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Pichli baar add kiya tha (jab pura User object ho)
    Optional<Wallet> findByUser(User user);
    Optional<Wallet> findByUserEmail(String email);

    // YEH NAYI LINE ADD KAR: Jab sirf userId (number) ho
    Optional<Wallet> findByUserId(Long userId);
}