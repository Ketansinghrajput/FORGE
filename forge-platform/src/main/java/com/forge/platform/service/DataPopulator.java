package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.UserRole;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataPopulator implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional // SENSEI: Transactional is mandatory taaki partial data save na ho
    public void run(String... args) throws Exception {
        log.info("🚀 Starting Database Population...");

        // 1. Pehle check karo ki user exist karta hai ya nahi (to avoid duplicate errors)
        if (userRepository.findByEmail("sensei@forge.com").isEmpty()) {

            // 2. CREATE AND SAVE USER FIRST (Master table)
            User sensei = User.builder()
                    .fullName("Ketan Singh")
                    .email("sensei@forge.com")
                    .password("password123") // Ideal scenario mein BCrypt password hona chahiye
                    .role(UserRole.USER)
                    .build();

            // SavedUser ke paas ab DB se aayi hui real ID hogi
            User savedUser = userRepository.save(sensei);
            log.info("✅ User created with ID: {}", savedUser.getId());

            // 3. CREATE AND SAVE WALLET (Dependent table)
            // Ab hum savedUser ka reference de rahe hain, isliye FK violation nahi hoga
            Wallet senseiWallet = Wallet.builder()
                    .user(savedUser)
                    .totalBalance(new BigDecimal("14500.00"))
                    .lockedAmount(BigDecimal.ZERO)
                    .version(0L) // Optimistic locking initial state
                    .build();

            walletRepository.save(senseiWallet);
            log.info("✅ Wallet created for User: {}", savedUser.getEmail());

            log.info("🏁 Database setup complete for Sensei!");
        } else {
            log.info("ℹ️ User already exists, skipping population.");
        }
    }
}