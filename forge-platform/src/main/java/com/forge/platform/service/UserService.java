package com.forge.platform.service;

import com.forge.platform.dto.UserRequestDto;
import com.forge.platform.dto.UserResponseDto;
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
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        // ✅ Source of Truth: Naya wallet reset state mein
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .totalBalance(BigDecimal.ZERO)
                .lockedAmount(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
        log.info("Wallet initialized for user: {}", savedUser.getEmail());
        return savedUser;
    }

    @Transactional
    public UserResponseDto updateProfile(User user, UserRequestDto dto) {
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        User saved = userRepository.save(user);

        // 🔥 SENSEI FIX: Yahan WalletRepository se live balance fetch karo
        BigDecimal liveBalance = walletRepository.findByUser(saved)
                .map(Wallet::getTotalBalance)
                .orElse(BigDecimal.ZERO);

        return new UserResponseDto(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                liveBalance, // 👈 Passing live balance from Wallet table
                saved.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(User user, UserRequestDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public BigDecimal getUserAvailableBalance(Long userId) {
        // Updated query to use specific method
        return walletRepository.findByUserId(userId)
                .map(Wallet::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }
}