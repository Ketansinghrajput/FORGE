package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        log.info("Registering new user: {}", user.getEmail());

        // 1. Password Encoding (Barclays Security Standard)
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Save User to get the generated ID
        User savedUser = userRepository.save(user);

        // 3. Create a default Wallet for the new user (Escrow ready)
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .totalBalance(BigDecimal.ZERO) // New users start with 0
                .lockedAmount(BigDecimal.ZERO)
                .build();

        walletRepository.save(wallet);
        log.info("Wallet initialized for user: {}", savedUser.getEmail());

        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Pro-Tip: Agar balance check karna hai toh Wallet check karo, User nahi.
    public BigDecimal getUserAvailableBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }
}