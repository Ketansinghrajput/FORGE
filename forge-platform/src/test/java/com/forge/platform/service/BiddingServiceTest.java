package com.forge.platform.service;

import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private BidRepository bidRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private WalletService walletService;

    @InjectMocks private BiddingService biddingService;

    private User bidder;
    private Wallet bidderWallet;
    private Auction auction;

    @BeforeEach
    void setUp() {
        bidder = User.builder().id(1L).email("sensei@forge.com").fullName("Ketan Singh").build();
        bidderWallet = Wallet.builder().user(bidder).totalBalance(new BigDecimal("50000")).lockedAmount(BigDecimal.ZERO).build();

        auction = Auction.builder()
                .id(100L)
                .startingPrice(new BigDecimal("1000"))
                .status(AuctionStatus.ACTIVE)
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        // 🔥 SENSEI FIX: Forcefully injecting the walletService mock bypassing constructor
        org.springframework.test.util.ReflectionTestUtils.setField(biddingService, "walletService", walletService);
    }

    @Test
    void placeBid_ShouldThrowException_WhenAuctionNotFound() {
        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> biddingService.placeBid(100L, bidder, BigDecimal.TEN));
    }

    @Test
    void placeBid_ShouldThrowException_WhenWalletNotFound() {
        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> biddingService.placeBid(100L, bidder, BigDecimal.TEN));
    }

    @Test
    void placeBid_ShouldFailValidation_WhenAuctionNotActive() {
        auction.setStatus(AuctionStatus.CLOSED);
        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));

        assertThrows(IllegalStateException.class, () -> biddingService.placeBid(100L, bidder, new BigDecimal("2000")));
    }

    @Test
    void placeBid_ShouldSucceed_AndRefundPreviousBidder() {
        // Setup Previous Bidder
        User prevBidder = User.builder().id(2L).email("prev@forge.com").build();
        Wallet prevWallet = Wallet.builder().user(prevBidder).totalBalance(new BigDecimal("10000")).lockedAmount(new BigDecimal("1500")).build();

        auction.setHighestBidder(prevBidder);
        auction.setCurrentHighestBid(new BigDecimal("1500"));

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));
        when(walletRepository.findByUser(prevBidder)).thenReturn(Optional.of(prevWallet));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArguments()[0]);
        when(walletService.getWalletByUserId(1L)).thenReturn(bidderWallet);

        // Act
        Bid result = biddingService.placeBid(100L, bidder, new BigDecimal("2000"));

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("2000"), auction.getCurrentHighestBid());
        assertEquals(bidder, auction.getHighestBidder());

        // Verify refund logic
        verify(walletRepository).save(prevWallet);
        assertEquals(BigDecimal.ZERO, prevWallet.getLockedAmount());

        // Verify lock logic for new bidder
        verify(walletRepository).save(bidderWallet);
        assertEquals(new BigDecimal("2000"), bidderWallet.getLockedAmount());

        // Verify broadcast
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }
}