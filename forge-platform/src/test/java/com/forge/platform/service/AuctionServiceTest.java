package com.forge.platform.service;

import com.forge.platform.dto.AuctionRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import com.forge.platform.repository.BidRepository;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletService walletService;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AuctionService auctionService;

    private User seller;
    private User bidder;
    private Auction activeAuction;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(1L)
                .email("seller@forge.com")
                .fullName("Seller One")
                .build();

        bidder = User.builder()
                .id(2L)
                .email("bidder@forge.com")
                .fullName("Bidder One")
                .build();

        activeAuction = Auction.builder()
                .id(10L)
                .title("Vintage Watch")
                .description("A rare vintage watch")
                .startingPrice(new BigDecimal("1000.00"))
                .currentHighestBid(new BigDecimal("1000.00"))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .build();
    }

    // ─── createAuction ────────────────────────────────────────────────────────

    @Test
    void createAuction_shouldSaveAndReturnAuction_whenRequestIsValid() {
        AuctionRequest request = new AuctionRequest(
                "Vintage Watch",
                "A rare vintage watch",
                new BigDecimal("1000.00"),
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusHours(3),
                "http://image.url/watch.jpg"
        );

        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        Auction result = auctionService.createAuction(request, seller);

        assertNotNull(result);
        assertEquals("Vintage Watch", result.getTitle());
        assertEquals(AuctionStatus.PLANNED, result.getStatus()); // future startTime → PLANNED
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    void createAuction_shouldSetStatusActive_whenStartTimeIsNow() {
        AuctionRequest request = new AuctionRequest(
                "Live Auction",
                "Starts now",
                new BigDecimal("500.00"),
                LocalDateTime.now().minusSeconds(30), // already started
                LocalDateTime.now().plusHours(1),
                null
        );

        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        Auction result = auctionService.createAuction(request, seller);

        assertEquals(AuctionStatus.ACTIVE, result.getStatus());
    }

    @Test
    void createAuction_shouldThrow_whenStartTimeIsInPast() {
        AuctionRequest request = new AuctionRequest(
                "Bad Auction",
                "desc",
                new BigDecimal("100.00"),
                LocalDateTime.now().minusHours(2), // far in the past
                LocalDateTime.now().plusHours(1),
                null
        );

        assertThrows(IllegalArgumentException.class,
                () -> auctionService.createAuction(request, seller));

        verify(auctionRepository, never()).save(any());
    }

    @Test
    void createAuction_shouldThrow_whenEndTimeIsBeforeStartTime() {
        AuctionRequest request = new AuctionRequest(
                "Bad Auction",
                "desc",
                new BigDecimal("100.00"),
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(1), // end before start
                null
        );

        assertThrows(IllegalArgumentException.class,
                () -> auctionService.createAuction(request, seller));

        verify(auctionRepository, never()).save(any());
    }

    // ─── placeBid ─────────────────────────────────────────────────────────────

    @Test
    void placeBid_shouldSucceed_whenBidIsHigherAndAuctionIsActive() {
        BigDecimal bidAmount = new BigDecimal("1500.00");

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));
        when(auctionRepository.saveAndFlush(any())).thenReturn(activeAuction);
        when(bidRepository.save(any())).thenReturn(new Bid());

        auctionService.placeBid(10L, bidder, bidAmount);

        verify(walletService).lockFunds(bidder, bidAmount);
        verify(auctionRepository).saveAndFlush(any());
        verify(bidRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/10"), any(Map.class));
        assertEquals(bidAmount, activeAuction.getCurrentHighestBid());
        assertEquals(bidder, activeAuction.getHighestBidder());
    }

    @Test
    void placeBid_shouldRefundPreviousBidder_whenOutbid() {
        User previousBidder = User.builder()
                .id(3L)
                .email("prev@forge.com")
                .fullName("Previous Bidder")
                .build();

        activeAuction.setHighestBidder(previousBidder);
        activeAuction.setCurrentHighestBid(new BigDecimal("1200.00"));

        BigDecimal newBidAmount = new BigDecimal("1500.00");

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));
        when(auctionRepository.saveAndFlush(any())).thenReturn(activeAuction);
        when(bidRepository.save(any())).thenReturn(new Bid());

        auctionService.placeBid(10L, bidder, newBidAmount);

        verify(walletService).lockFunds(bidder, newBidAmount);
        verify(walletService).unlockFunds(previousBidder, new BigDecimal("1200.00"));
        verify(notificationService).sendOutbidEmail(
                eq("prev@forge.com"), eq("Vintage Watch"), eq(newBidAmount));
    }

    @Test
    void placeBid_shouldThrow_whenAuctionNotActive() {
        activeAuction.setStatus(AuctionStatus.COMPLETED);

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));

        assertThrows(IllegalStateException.class,
                () -> auctionService.placeBid(10L, bidder, new BigDecimal("1500.00")));

        verify(walletService, never()).lockFunds(any(), any());
    }

    @Test
    void placeBid_shouldThrow_whenBidNotHigherThanCurrent() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));

        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(10L, bidder, new BigDecimal("1000.00")));

        verify(walletService, never()).lockFunds(any(), any());
    }

    @Test
    void placeBid_shouldThrow_whenSellerBidsOnOwnAuction() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));

        assertThrows(IllegalArgumentException.class,
                () -> auctionService.placeBid(10L, seller, new BigDecimal("1500.00")));

        verify(walletService, never()).lockFunds(any(), any());
    }

    @Test
    void placeBid_shouldThrow_whenAuctionNotFound() {
        when(auctionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> auctionService.placeBid(99L, bidder, new BigDecimal("1500.00")));
    }

    @Test
    void placeBid_byEmail_shouldResolveUserAndPlaceBid() {
        BigDecimal bidAmount = new BigDecimal("1500.00");

        when(userRepository.findByEmail("bidder@forge.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));
        when(auctionRepository.saveAndFlush(any())).thenReturn(activeAuction);
        when(bidRepository.save(any())).thenReturn(new Bid());

        auctionService.placeBid(10L, "bidder@forge.com", bidAmount);

        verify(userRepository).findByEmail("bidder@forge.com");
        verify(walletService).lockFunds(bidder, bidAmount);
    }

    // ─── processExpiredAuctions ───────────────────────────────────────────────

    @Test
    void processExpiredAuctions_shouldSettleAndMarkCompleted_whenWinnerExists() {
        activeAuction.setHighestBidder(bidder);
        activeAuction.setCurrentHighestBid(new BigDecimal("2000.00"));

        Bid winningBid = Bid.builder()
                .id(1L)
                .auction(activeAuction)
                .bidder(bidder)
                .amount(new BigDecimal("2000.00"))
                .successful(false)
                .build();

        when(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(List.of(activeAuction));
        when(bidRepository.findHighestBidForAuction(10L)).thenReturn(Optional.of(winningBid));
        when(auctionRepository.saveAndFlush(any())).thenReturn(activeAuction);

        auctionService.processExpiredAuctions();

        verify(walletService).settleAuction(bidder, seller, new BigDecimal("2000.00"));
        assertEquals(AuctionStatus.COMPLETED, activeAuction.getStatus());
        assertTrue(winningBid.isSuccessful());
        verify(notificationService).sendAuctionWonEmail(
                eq("bidder@forge.com"), eq("Vintage Watch"), eq(new BigDecimal("2000.00")));
    }

    @Test
    void processExpiredAuctions_shouldMarkExpired_whenNoBidder() {
        activeAuction.setHighestBidder(null);

        when(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(List.of(activeAuction));
        when(auctionRepository.saveAndFlush(any())).thenReturn(activeAuction);

        auctionService.processExpiredAuctions();

        assertEquals(AuctionStatus.EXPIRED, activeAuction.getStatus());
        verify(walletService, never()).settleAuction(any(), any(), any());
        verify(notificationService, never()).sendAuctionWonEmail(any(), any(), any());
    }

    @Test
    void processExpiredAuctions_shouldContinue_whenOneAuctionFails() {
        Auction badAuction = Auction.builder()
                .id(99L)
                .status(AuctionStatus.ACTIVE)
                .highestBidder(bidder)
                .currentHighestBid(new BigDecimal("500.00"))
                .seller(seller)
                .title("Bad Auction")
                .build();

        when(auctionRepository.findByStatusAndEndTimeBefore(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(List.of(badAuction));

        // walletService throws — exception fires before saveAndFlush is reached
        doThrow(new RuntimeException("Payment failed"))
                .when(walletService).settleAuction(any(), any(), any());

        // should NOT throw — error is caught and logged internally
        assertDoesNotThrow(() -> auctionService.processExpiredAuctions());
    }

    // ─── deleteAuction ────────────────────────────────────────────────────────

    @Test
    void deleteAuction_shouldDeleteBidsAndAuction_whenSellerOwnsIt() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));

        auctionService.deleteAuction(10L, seller);

        verify(bidRepository).deleteByAuctionId(10L);
        verify(auctionRepository).deleteById(10L);
    }

    @Test
    void deleteAuction_shouldThrow_whenNonOwnerTriesToDelete() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));

        assertThrows(IllegalStateException.class,
                () -> auctionService.deleteAuction(10L, bidder));

        verify(auctionRepository, never()).deleteById(any());
    }

    // ─── getActiveAuctionsPaginated ───────────────────────────────────────────

    @Test
    void getActiveAuctionsPaginated_shouldReturnPagedResponse() {
        Page<Auction> mockPage = new PageImpl<>(List.of(activeAuction));

        when(auctionRepository.findByStatusIn(anyList(), any(Pageable.class)))
                .thenReturn(mockPage);

        Map<String, Object> result = auctionService.getActiveAuctionsPaginated(0, 10);

        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        assertTrue(result.containsKey("totalPages"));
        assertTrue(result.containsKey("totalElements"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertEquals(1, content.size());
        assertEquals("Vintage Watch", content.get(0).get("title"));
        assertEquals("ACTIVE", content.get(0).get("status"));
    }

    // ─── getInitialAuctionState ───────────────────────────────────────────────

    @Test
    void getInitialAuctionState_shouldReturnFullAuctionMap() {
        Wallet wallet = Wallet.builder()
                .totalBalance(new BigDecimal("5000.00"))
                .build();

        Bid pastBid = Bid.builder()
                .amount(new BigDecimal("1000.00"))
                .bidder(bidder)
                .build();

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(activeAuction));
        when(walletRepository.findByUserEmail("bidder@forge.com")).thenReturn(Optional.of(wallet));
        when(bidRepository.findByAuctionIdOrderByAmountDesc(10L)).thenReturn(List.of(pastBid));

        Map<String, Object> result = auctionService.getInitialAuctionState(10L, "bidder@forge.com");

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.get("currentBid"));
        assertEquals("Waiting for Bids...", result.get("highestBidder"));
        assertEquals(new BigDecimal("5000.00"), result.get("availableFunds"));
        assertEquals("Vintage Watch", result.get("title"));
        assertNotNull(result.get("history"));
    }
}