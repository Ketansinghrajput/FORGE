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
    // FIX: removed @Mock WalletService — BiddingService doesn't have walletService field

    @InjectMocks private BiddingService biddingService;

    private User bidder;
    private Wallet bidderWallet;
    private Auction auction;

    @BeforeEach
    void setUp() {
        bidder = User.builder().id(1L).email("sensei@forge.com").fullName("Ketan Singh").build();
        bidderWallet = Wallet.builder()
                .user(bidder)
                .totalBalance(new BigDecimal("50000"))
                .lockedAmount(BigDecimal.ZERO)
                .version(0)
                .build();

        auction = Auction.builder()
                .id(100L)
                .startingPrice(new BigDecimal("1000"))
                .status(AuctionStatus.ACTIVE)
                .endTime(LocalDateTime.now().plusDays(1))
                .build();
        // FIX: removed ReflectionTestUtils.setField — walletService doesn't exist in BiddingService
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

    // Case C: First bid ever on auction
    @Test
    void placeBid_ShouldSucceed_WhenFirstBidOnAuction() {
        // auction has no previous bidder
        auction.setHighestBidder(null);
        auction.setCurrentHighestBid(null);

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArguments()[0]);

        Bid result = biddingService.placeBid(100L, bidder, new BigDecimal("2000"));

        assertNotNull(result);
        assertEquals(bidder, auction.getHighestBidder());
        assertEquals(new BigDecimal("2000"), auction.getCurrentHighestBid());
        // Full bid amount locked
        assertEquals(new BigDecimal("2000"), bidderWallet.getLockedAmount());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    // Case B: Same user raises their own bid
    @Test
    void placeBid_ShouldSucceed_WhenSameUserRaisesBid() {
        auction.setHighestBidder(bidder); // same user
        auction.setCurrentHighestBid(new BigDecimal("1500"));
        bidderWallet.setLockedAmount(new BigDecimal("1500")); // already locked

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArguments()[0]);

        Bid result = biddingService.placeBid(100L, bidder, new BigDecimal("2500"));

        assertNotNull(result);
        // Only difference (2500 - 1500 = 1000) should be additionally locked
        assertEquals(new BigDecimal("2500"), bidderWallet.getLockedAmount());
        verify(walletRepository, times(1)).save(bidderWallet); // only bidder wallet saved, no prev wallet
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void placeBid_ShouldThrowException_WhenBidTooLow() {
        auction.setCurrentHighestBid(new BigDecimal("3000"));

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));

        assertThrows(IllegalArgumentException.class,
                () -> biddingService.placeBid(100L, bidder, new BigDecimal("2000")));
    }

    @Test
    void placeBid_ShouldThrowException_WhenInsufficientWalletBalance() {
        bidderWallet.setTotalBalance(new BigDecimal("500")); // not enough

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));

        assertThrows(IllegalArgumentException.class,
                () -> biddingService.placeBid(100L, bidder, new BigDecimal("2000")));
    }

    @Test
    void placeBid_ShouldExtendAuctionEndTime_WhenBidInLastMinute() {
        auction.setEndTime(LocalDateTime.now().plusSeconds(30)); // last 30 seconds
        LocalDateTime originalEnd = auction.getEndTime();

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArguments()[0]);

        biddingService.placeBid(100L, bidder, new BigDecimal("2000"));

        // End time should be extended by 5 minutes
        assertTrue(auction.getEndTime().isAfter(originalEnd));
    }

    @Test
    void placeBid_ShouldSucceed_AndRefundPreviousBidder() {
        User prevBidder = User.builder().id(2L).email("prev@forge.com").build();
        Wallet prevWallet = Wallet.builder()
                .user(prevBidder)
                .totalBalance(new BigDecimal("10000"))
                .lockedAmount(new BigDecimal("1500"))
                .version(0)
                .build();

        auction.setHighestBidder(prevBidder);
        auction.setCurrentHighestBid(new BigDecimal("1500"));

        when(auctionRepository.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(walletRepository.findByUser(bidder)).thenReturn(Optional.of(bidderWallet));
        when(walletRepository.findByUser(prevBidder)).thenReturn(Optional.of(prevWallet));
        when(bidRepository.save(any(Bid.class))).thenAnswer(i -> i.getArguments()[0]);

        Bid result = biddingService.placeBid(100L, bidder, new BigDecimal("2000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("2000"), auction.getCurrentHighestBid());
        assertEquals(bidder, auction.getHighestBidder());

        verify(walletRepository).save(prevWallet);
        assertEquals(BigDecimal.ZERO, prevWallet.getLockedAmount());

        verify(walletRepository).save(bidderWallet);
        assertEquals(new BigDecimal("2000"), bidderWallet.getLockedAmount());

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }
}