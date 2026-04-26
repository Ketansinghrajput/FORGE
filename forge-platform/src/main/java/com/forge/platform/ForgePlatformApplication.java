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

import java.math.BigDecimal;
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
    CommandLineRunner initData(UserRepository userRepository, WalletRepository walletRepository) {
        return args -> {
            // User 3 check
            if (!userRepository.existsById(3L)) {
                User tester = User.builder()
                        .id(3L)
                        .email("tester@forge.com")
                        .fullName("Tester Don")
                        .password("password123")
                        .role(UserRole.USER)
                        .build();

                // Pehle User ko DB mein flush karo 🔑
                userRepository.saveAndFlush(tester);

                Wallet wallet = Wallet.builder()
                        .id(3L)
                        .user(tester)
                        .totalBalance(new BigDecimal("100000"))
                        .lockedAmount(BigDecimal.ZERO)
                        .version(0L)
                        .build();

                walletRepository.saveAndFlush(wallet);
                System.out.println("✅ SUCCESS: USER 3 & WALLET CREATED BY HIBERNATE!");
            } else {
                System.out.println("ℹ️ INFO: USER 3 ALREADY EXISTS IN DB.");
            }
        };
    }
}