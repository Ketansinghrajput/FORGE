package com.forge.platform.service;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionManagerServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private BiddingEngine engine;
    @Mock private WalletService walletService;

    @InjectMocks private AuctionManagerService service;

    private Auction buildAuction(Long id, AuctionStatus status, User seller, User highestBidder, BigDecimal bid) {
        Auction a = new Auction();
        a.setId(id);
        a.setTitle("Test Auction " + id);
        a.setStatus(status);
        a.setStartingPrice(new BigDecimal("1000.00"));
        a.setSeller(seller);
        a.setHighestBidder(highestBidder);
        a.setCurrentHighestBid(bid);
        a.setStartTime(LocalDateTime.now().minusMinutes(10));
        a.setEndTime(LocalDateTime.now().minusMinutes(1));
        return a;
    }

    private User buildUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFullName("User " + id);
        return u;
    }

    // ── startPendingAuctions ──────────────────────────────────────────────────

    @Test
    void startPendingAuctions_setsStatusToActive_andRegistersInEngine() {
        Auction auction = buildAuction(1L, AuctionStatus.PENDING, buildUser(1L, "seller@forge.com"), null, null);
        when(auctionRepository.findPendingAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepository.save(any())).thenReturn(auction);

        service.startPendingAuctions();

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        verify(auctionRepository).save(auction);
        verify(engine).registerAuction(eq(1L), any());
    }

    @Test
    void startPendingAuctions_registersCorrectAuctionId_perAuction() {
        Auction a1 = buildAuction(10L, AuctionStatus.PENDING, buildUser(1L, "s1@forge.com"), null, null);
        Auction a2 = buildAuction(20L, AuctionStatus.PENDING, buildUser(2L, "s2@forge.com"), null, null);
        when(auctionRepository.findPendingAuctions(any())).thenReturn(List.of(a1, a2));
        when(auctionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.startPendingAuctions();

        verify(engine).registerAuction(eq(10L), any());
        verify(engine).registerAuction(eq(20L), any());
        verify(auctionRepository, times(2)).save(any());
    }

    @Test
    void startPendingAuctions_doesNothing_whenNoPendingAuctions() {
        when(auctionRepository.findPendingAuctions(any())).thenReturn(List.of());

        service.startPendingAuctions();

        verifyNoInteractions(engine);
        verify(auctionRepository, never()).save(any());
    }

    // ── closeExpiredAuctions ──────────────────────────────────────────────────

    @Test
    void closeExpiredAuctions_setsStatusToClosed_andUnregistersEngine() {
        User seller = buildUser(1L, "seller@forge.com");
        Auction auction = buildAuction(5L, AuctionStatus.ACTIVE, seller, null, null);
        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepository.save(any())).thenReturn(auction);

        service.closeExpiredAuctions();

        assertEquals(AuctionStatus.CLOSED, auction.getStatus());
        verify(auctionRepository).save(auction);
        verify(engine).unregisterAuction(5L);
    }

    @Test
    void closeExpiredAuctions_settlesWallet_whenWinnerExists() {
        User seller = buildUser(1L, "seller@forge.com");
        User winner = buildUser(2L, "winner@forge.com");
        BigDecimal finalPrice = new BigDecimal("5000.00");

        Auction auction = buildAuction(6L, AuctionStatus.ACTIVE, seller, winner, finalPrice);
        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepository.save(any())).thenReturn(auction);

        service.closeExpiredAuctions();

        verify(walletService).settleAuction(winner, seller, finalPrice);
        assertEquals(AuctionStatus.CLOSED, auction.getStatus());
    }

    @Test
    void closeExpiredAuctions_skipsSettlement_whenNoWinner() {
        User seller = buildUser(1L, "seller@forge.com");
        Auction auction = buildAuction(7L, AuctionStatus.ACTIVE, seller, null, null); // no bidder

        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepository.save(any())).thenReturn(auction);

        service.closeExpiredAuctions();

        verifyNoInteractions(walletService);
        verify(engine).unregisterAuction(7L);
        assertEquals(AuctionStatus.CLOSED, auction.getStatus());
    }

    @Test
    void closeExpiredAuctions_continuesClosing_whenSettlementFails() {
        User seller = buildUser(1L, "seller@forge.com");
        User winner = buildUser(2L, "winner@forge.com");

        Auction a1 = buildAuction(8L, AuctionStatus.ACTIVE, seller, winner, new BigDecimal("3000.00"));
        Auction a2 = buildAuction(9L, AuctionStatus.ACTIVE, seller, null, null);

        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of(a1, a2));
        when(auctionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("Settlement failed"))
                .when(walletService).settleAuction(any(), any(), any());

        // Should not throw — error is caught internally
        assertDoesNotThrow(() -> service.closeExpiredAuctions());

        // Both auctions still closed and unregistered
        assertEquals(AuctionStatus.CLOSED, a1.getStatus());
        assertEquals(AuctionStatus.CLOSED, a2.getStatus());
        verify(engine).unregisterAuction(8L);
        verify(engine).unregisterAuction(9L);
    }

    @Test
    void closeExpiredAuctions_settlesCorrectAmounts_perAuction() {
        User seller = buildUser(1L, "seller@forge.com");
        User w1 = buildUser(2L, "w1@forge.com");
        User w2 = buildUser(3L, "w2@forge.com");

        Auction a1 = buildAuction(10L, AuctionStatus.ACTIVE, seller, w1, new BigDecimal("2000.00"));
        Auction a2 = buildAuction(11L, AuctionStatus.ACTIVE, seller, w2, new BigDecimal("8000.00"));

        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of(a1, a2));
        when(auctionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.closeExpiredAuctions();

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(walletService, times(2)).settleAuction(any(), eq(seller), amountCaptor.capture());

        List<BigDecimal> amounts = amountCaptor.getAllValues();
        assertTrue(amounts.contains(new BigDecimal("2000.00")));
        assertTrue(amounts.contains(new BigDecimal("8000.00")));
    }

    @Test
    void closeExpiredAuctions_doesNothing_whenNoExpiredAuctions() {
        when(auctionRepository.findExpiredAuctions(any())).thenReturn(List.of());

        service.closeExpiredAuctions();

        verifyNoInteractions(engine, walletService);
        verify(auctionRepository, never()).save(any());
    }
}