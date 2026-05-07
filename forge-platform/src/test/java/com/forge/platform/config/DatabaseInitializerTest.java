package com.forge.platform.config;

import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializerTest {

    @Mock private UserRepository userRepo;
    @Mock private WalletRepository walletRepo;
    @Mock private AuctionRepository auctionRepo;

    private DatabaseInitializer databaseInitializer;

    @BeforeEach
    void setUp() {
        // 🔥 SENSEI FIX: Manually creating the instance to force IDE to find the class
        databaseInitializer = new DatabaseInitializer();
    }

    @Test
    void initDatabase_WhenDbEmpty_ShouldSeedData() throws Exception {
        when(userRepo.count()).thenReturn(0L);

        CommandLineRunner runner = databaseInitializer.initDatabase(userRepo, walletRepo, auctionRepo);
        runner.run();

        verify(userRepo, times(2)).save(any());
        verify(walletRepo, times(2)).save(any());
        verify(auctionRepo, times(1)).save(any());
    }

    @Test
    void initDatabase_WhenDbNotEmpty_ShouldSkipSeeding() throws Exception {
        when(userRepo.count()).thenReturn(5L);

        CommandLineRunner runner = databaseInitializer.initDatabase(userRepo, walletRepo, auctionRepo);
        runner.run();

        verify(userRepo, never()).save(any());
        verify(walletRepo, never()).save(any());
        verify(auctionRepo, never()).save(any());
    }
}