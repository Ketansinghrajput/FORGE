package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WalletService walletService;

    @Test
    void getMyBalance_Success() {
        String email = "sensei@forge.com";
        User user = new User();
        Wallet wallet = mock(Wallet.class);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1000));

        Map<String, BigDecimal> result = walletService.getMyBalance(email);

        assertEquals(BigDecimal.valueOf(1000), result.get("balance"));
    }

    @Test
    void getMyBalance_UserNotFound_ThrowsException() {
        String email = "ghost@forge.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> walletService.getMyBalance(email));
        assertEquals("User not found!", exception.getMessage());
    }

    @Test
    void topUpWallet_Success() {
        String email = "sensei@forge.com";
        BigDecimal topUpAmount = BigDecimal.valueOf(500);
        User user = new User();
        user.setEmail(email);

        Wallet wallet = mock(Wallet.class);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserWithLock(user)).thenReturn(Optional.of(wallet));
        when(wallet.getTotalBalance()).thenReturn(BigDecimal.valueOf(1000));
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1500));

        walletService.topUpWallet(email, topUpAmount);

        verify(wallet).setTotalBalance(BigDecimal.valueOf(1500));
        verify(walletRepository).saveAndFlush(wallet);
        verify(messagingTemplate).convertAndSendToUser(eq(email), eq("/queue/wallet"), anyMap());
    }

    @Test
    void lockFunds_Success() {
        User user = new User();
        user.setEmail("sensei@forge.com");
        BigDecimal lockAmount = BigDecimal.valueOf(200);

        Wallet wallet = mock(Wallet.class);

        when(walletRepository.findByUserWithLock(user)).thenReturn(Optional.of(wallet));
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1000)); // Sufficient balance
        when(wallet.getLockedAmount()).thenReturn(BigDecimal.valueOf(100));

        walletService.lockFunds(user, lockAmount);

        verify(wallet).setLockedAmount(BigDecimal.valueOf(300));
        verify(walletRepository).saveAndFlush(wallet);
        verify(messagingTemplate).convertAndSendToUser(eq(user.getEmail()), eq("/queue/wallet"), anyMap());
    }

    @Test
    void lockFunds_InsufficientFunds_ThrowsException() {
        User user = new User();
        BigDecimal lockAmount = BigDecimal.valueOf(2000);

        Wallet wallet = mock(Wallet.class);

        when(walletRepository.findByUserWithLock(user)).thenReturn(Optional.of(wallet));
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(500)); // Insufficient

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> walletService.lockFunds(user, lockAmount));
        assertTrue(exception.getMessage().contains("Insufficient funds!"));

        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void unlockFunds_Success() {
        User user = new User();
        user.setEmail("sensei@forge.com");
        BigDecimal unlockAmount = BigDecimal.valueOf(200);

        Wallet wallet = mock(Wallet.class);

        when(walletRepository.findByUserWithLock(user)).thenReturn(Optional.of(wallet));
        when(wallet.getLockedAmount()).thenReturn(BigDecimal.valueOf(500));
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1000));

        walletService.unlockFunds(user, unlockAmount);

        verify(wallet).setLockedAmount(BigDecimal.valueOf(300));
        verify(walletRepository).saveAndFlush(wallet);
        verify(messagingTemplate).convertAndSendToUser(eq(user.getEmail()), eq("/queue/wallet"), anyMap());
    }

    @Test
    void unlockFunds_BelowZero_SetsToZero() {
        User user = new User();
        user.setEmail("sensei@forge.com");
        BigDecimal unlockAmount = BigDecimal.valueOf(500);

        Wallet wallet = mock(Wallet.class);

        when(walletRepository.findByUserWithLock(user)).thenReturn(Optional.of(wallet));
        when(wallet.getLockedAmount()).thenReturn(BigDecimal.valueOf(200)); // Unlocking more than locked
        when(wallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1000));

        walletService.unlockFunds(user, unlockAmount);

        verify(wallet).setLockedAmount(BigDecimal.ZERO); // Should floor at 0
        verify(walletRepository).saveAndFlush(wallet);
    }

    @Test
    void settleAuction_Success() {
        User winner = new User();
        winner.setEmail("winner@forge.com");
        User seller = new User();
        seller.setEmail("seller@forge.com");
        BigDecimal auctionAmount = BigDecimal.valueOf(500);

        Wallet winnerWallet = mock(Wallet.class);
        Wallet sellerWallet = mock(Wallet.class);

        // Mock Winner
        when(walletRepository.findByUserWithLock(winner)).thenReturn(Optional.of(winnerWallet));
        when(winnerWallet.getTotalBalance()).thenReturn(BigDecimal.valueOf(2000));
        when(winnerWallet.getLockedAmount()).thenReturn(BigDecimal.valueOf(500));
        when(winnerWallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1500));

        // Mock Seller
        when(walletRepository.findByUserWithLock(seller)).thenReturn(Optional.of(sellerWallet));
        when(sellerWallet.getTotalBalance()).thenReturn(BigDecimal.valueOf(1000));
        when(sellerWallet.getAvailableBalance()).thenReturn(BigDecimal.valueOf(1500));

        walletService.settleAuction(winner, seller, auctionAmount);

        // Winner Assertions
        verify(winnerWallet).setTotalBalance(BigDecimal.valueOf(1500)); // 2000 - 500
        verify(winnerWallet).setLockedAmount(BigDecimal.ZERO); // 500 - 500
        verify(walletRepository).saveAndFlush(winnerWallet);

        // Seller Assertions
        verify(sellerWallet).setTotalBalance(BigDecimal.valueOf(1500)); // 1000 + 500
        verify(walletRepository).saveAndFlush(sellerWallet);

        // Broadcast Assertions (1 for winner, 1 for seller)
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/wallet"), anyMap());
    }

    @Test
    void settleAuction_WinnerBalanceDropped_ThrowsException() {
        User winner = new User();
        User seller = new User();
        BigDecimal auctionAmount = BigDecimal.valueOf(1000);

        Wallet winnerWallet = mock(Wallet.class);

        when(walletRepository.findByUserWithLock(winner)).thenReturn(Optional.of(winnerWallet));
        when(winnerWallet.getTotalBalance()).thenReturn(BigDecimal.valueOf(500)); // Balance less than auction price

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> walletService.settleAuction(winner, seller, auctionAmount));
        assertTrue(exception.getMessage().contains("Critical Error"));

        verify(walletRepository, never()).saveAndFlush(any());
    }

    @Test
    void getWalletByUserId_Success() {
        Long userId = 1L;
        Wallet wallet = mock(Wallet.class);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId(userId);

        assertNotNull(result);
        assertEquals(wallet, result);
    }
}