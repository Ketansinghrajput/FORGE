package com.forge.platform;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.UserRole;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ Add this

import java.math.BigDecimal;
import java.util.Optional;
import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ForgePlatformApplication {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(ForgePlatformApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder // 🚀 SENSEI: Inject encoder here
    ) {
        return args -> {
            String testEmail = "tester@forge.com";

            Optional<User> existingUser = userRepository.findByEmail(testEmail);

            if (existingUser.isEmpty()) {
                User tester = User.builder()
                        .email(testEmail)
                        .fullName("Tester Don")
                        // ✅ SENSEI: Password must be encoded before saving to DB
                        .password(passwordEncoder.encode("password123"))
                        .role(UserRole.USER)
                        .build();

                User savedUser = userRepository.saveAndFlush(tester);
                System.out.println("✅ USER CREATED: " + savedUser.getEmail() + " with ID: " + savedUser.getId());

                Wallet wallet = Wallet.builder()
                        .user(savedUser)
                        .totalBalance(new BigDecimal("100000"))
                        .lockedAmount(BigDecimal.ZERO)
                        .version(0L)
                        .build();

                walletRepository.saveAndFlush(wallet);
                System.out.println("✅ WALLET CREATED FOR USER ID: " + savedUser.getId());
            } else {
                System.out.println("ℹ️ INFO: USER WITH EMAIL " + testEmail + " ALREADY EXISTS. SKIPPING INIT.");
            }
        };
    }
}