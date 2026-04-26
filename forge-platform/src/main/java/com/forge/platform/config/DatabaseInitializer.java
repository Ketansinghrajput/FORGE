package com.forge.platform.config;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepo,
            WalletRepository walletRepo,
            AuctionRepository auctionRepo) {
        return args -> {
            // Check if DB is empty to prevent primary key conflicts
            if (userRepo.count() == 0) {

                // 1. Create SELLER (Required because you can't bid on your own items)
                User seller = User.builder()
                        .email("seller@forge.com")
                        .password("Seller@123")
                        .fullName("Chacha Vidhayak")
                        .build();
                userRepo.save(seller);

                // 2. Create SENSEI (The Main Bidder)
                User sensei = User.builder()
                        .email("sensei@forge.com")
                        .password("Barclays@2026")
                        .fullName("Sensei Ketan")
                        .build();
                userRepo.save(sensei);

                // 3. Create Wallets
                Wallet sellerWallet = Wallet.builder()
                        .user(seller)
                        .totalBalance(new BigDecimal("5000.00"))
                        .lockedAmount(BigDecimal.ZERO)
                        .build();
                walletRepo.save(sellerWallet);

                Wallet senseiWallet = Wallet.builder()
                        .user(sensei)
                        .totalBalance(new BigDecimal("50000.00"))
                        .lockedAmount(BigDecimal.ZERO)
                        .build();
                walletRepo.save(senseiWallet);

                // 4. Create Dummy Auction (Rolex Submariner)
                // FIX: Added startTime to avoid PropertyValueException
                Auction auction = Auction.builder()
                        .title("Rolex Submariner Vintage")
                        .description("Testing Escrow Bidding Logic - High Performance Auction")
                        .startingPrice(new BigDecimal("10000.00"))
                        .currentHighestBid(new BigDecimal("10000.00"))
                        .startTime(LocalDateTime.now()) // Mandatory Field
                        .endTime(LocalDateTime.now().plusDays(7))
                        .status(AuctionStatus.ACTIVE)
                        .seller(seller) // Linked to Seller, NOT Sensei
                        .build();
                auctionRepo.save(auction);

                System.out.println("---");
                System.out.println("🟢 Persistence Ready: Forge DB Seeded Successfully!");
                System.out.println("🟢 Bidder: sensei@forge.com | Wallet: ₹50,000");
                System.out.println("🟢 Auction ID: 1 | Title: Rolex Submariner");
                System.out.println("---");
            } else {
                System.out.println("🟡 DB Initializer: Data already exists, skipping seed.");
            }
        };
    }
}