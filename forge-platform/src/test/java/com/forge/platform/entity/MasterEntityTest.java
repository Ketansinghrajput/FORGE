package com.forge.platform.entity;

import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MasterEntityTest {

    @Test
    void testUserEntity() {
        LocalDateTime now = LocalDateTime.now();

        // Test SuperBuilder & inherited fields
        User user1 = User.builder()
                .id(1L)
                .createdAt(now)
                .updatedAt(now)
                .email("sensei@forge.com")
                .password("Barclays@2026")
                .fullName("Sensei Ketan")
                .role(UserRole.USER)
                .build();

        // Test Setters
        User user2 = new User();
        user2.setId(2L);
        user2.setCreatedAt(now);
        user2.setUpdatedAt(now);
        user2.setEmail("test@forge.com");
        user2.setPassword("pass");
        user2.setFullName("Test User");
        user2.setRole(UserRole.ADMIN);

        // Test Getters
        assertEquals(1L, user1.getId());
        assertEquals(now, user1.getCreatedAt());
        assertEquals(now, user1.getUpdatedAt());
        assertEquals("sensei@forge.com", user1.getEmail());
        assertEquals("Barclays@2026", user1.getPassword());
        assertEquals("Sensei Ketan", user1.getFullName());
        assertEquals(UserRole.USER, user1.getRole());

        // Test UserDetails Custom Methods (Ye bohot coverage khate hain)
        assertEquals("sensei@forge.com", user1.getUsername());
        assertTrue(user1.isAccountNonExpired());
        assertTrue(user1.isAccountNonLocked());
        assertTrue(user1.isCredentialsNonExpired());
        assertTrue(user1.isEnabled());
        assertFalse(user1.getAuthorities().isEmpty());
        assertEquals("ROLE_USER", user1.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void testWalletEntity() {
        User dummyUser = new User();

        Wallet w1 = Wallet.builder()
                .id(10L)
                .user(dummyUser)
                .totalBalance(new BigDecimal("1000"))
                .lockedAmount(new BigDecimal("200"))
                .version(1L)
                .build();

        Wallet w2 = new Wallet();
        w2.setId(11L);
        w2.setUser(dummyUser);
        w2.setTotalBalance(BigDecimal.ZERO);
        w2.setLockedAmount(BigDecimal.ZERO);
        w2.setVersion(2L);

        assertEquals(10L, w1.getId());
        assertEquals(dummyUser, w1.getUser());
        assertEquals(new BigDecimal("1000"), w1.getTotalBalance());
        assertEquals(new BigDecimal("200"), w1.getLockedAmount());
        assertEquals(1L, w1.getVersion());

        // Test Custom Helper Method
        assertEquals(new BigDecimal("800"), w1.getAvailableBalance());
    }

    @Test
    void testAuctionEntity() {
        User seller = new User();
        User bidder = new User();
        LocalDateTime now = LocalDateTime.now();

        Auction a1 = Auction.builder()
                .id(100L)
                .title("Rolex Watch")
                .description("Vintage")
                .startingPrice(new BigDecimal("5000"))
                .currentHighestBid(new BigDecimal("6000"))
                .startTime(now)
                .endTime(now.plusDays(1))
                .imageUrl("http://img.com")
                .status(AuctionStatus.ACTIVE)
                .seller(seller)
                .highestBidder(bidder)
                .build();

        Auction a2 = new Auction();
        a2.setId(101L);
        a2.setTitle("MacBook");
        a2.setDescription("M3 Pro");
        a2.setStartingPrice(BigDecimal.TEN);
        // Manual setter test
        a2.setCurrentHighestBid(BigDecimal.valueOf(20));
        a2.setStartTime(now);
        a2.setEndTime(now.plusHours(2));
        a2.setImageUrl("img_url");
        a2.setStatus(AuctionStatus.CLOSED);
        a2.setSeller(seller);
        a2.setHighestBidder(bidder);

        assertEquals(100L, a1.getId());
        assertEquals("Rolex Watch", a1.getTitle());
        assertEquals("Vintage", a1.getDescription());
        assertEquals(new BigDecimal("5000"), a1.getStartingPrice());
        assertEquals(new BigDecimal("6000"), a1.getCurrentHighestBid());
        assertEquals(now, a1.getStartTime());
        assertNotNull(a1.getEndTime());
        assertEquals("http://img.com", a1.getImageUrl());
        assertEquals(AuctionStatus.ACTIVE, a1.getStatus());
        assertEquals(seller, a1.getSeller());
        assertEquals(bidder, a1.getHighestBidder());
    }

    @Test
    void testBidEntity() {
        Auction auction = new Auction();
        User bidder = new User();

        Bid b1 = Bid.builder()
                .id(50L)
                .auction(auction)
                .bidder(bidder)
                .amount(new BigDecimal("7000"))
                .successful(true)
                .build();

        Bid b2 = new Bid();
        b2.setId(51L);
        b2.setAuction(auction);
        b2.setBidder(bidder);
        b2.setAmount(new BigDecimal("1000"));
        b2.setSuccessful(false);

        assertEquals(50L, b1.getId());
        assertEquals(auction, b1.getAuction());
        assertEquals(bidder, b1.getBidder());
        assertEquals(new BigDecimal("7000"), b1.getAmount());
        assertTrue(b1.isSuccessful());
        assertFalse(b2.isSuccessful());
    }
}