package com.forge.platform.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MasterDtoTest {

    @Test
    void testAuctionRequest() {
        LocalDateTime now = LocalDateTime.now();
        AuctionRequest dto = new AuctionRequest("Rolex", "Vintage", BigDecimal.TEN, now, now.plusDays(1), "url");

        assertEquals("Rolex", dto.title());
        assertEquals("Vintage", dto.description());
        assertEquals(BigDecimal.TEN, dto.startingPrice());
        assertEquals(now, dto.startTime());
        assertNotNull(dto.endTime());
        assertEquals("url", dto.imageUrl());
    }

    @Test
    void testAuctionUpdateDTO() {
        // Constructor & Builder test
        AuctionUpdateDTO dto1 = new AuctionUpdateDTO(1L, BigDecimal.TEN, "bidder@test", BigDecimal.ZERO, "Ketan", "endTime", "timestamp");
        AuctionUpdateDTO dto2 = AuctionUpdateDTO.builder()
                .auctionId(1L)
                .newPrice(BigDecimal.TEN)
                .bidder("bidder@test")
                .availableFunds(BigDecimal.ZERO)
                .bidderName("Ketan")
                .endTime("endTime")
                .timestamp("timestamp")
                .build();

        AuctionUpdateDTO dto3 = new AuctionUpdateDTO();
        dto3.setAuctionId(2L);

        // Getter, Equals, Hashcode, ToString check
        assertEquals(1L, dto1.getAuctionId());
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertNotNull(dto1.hashCode());
        assertNotNull(dto1.toString());
    }

    @Test
    void testAuthResponse() {
        AuthResponse dto = new AuthResponse("1", "token123", "sensei@forge.com", "Sensei Ketan", "USER");
        assertEquals("1", dto.id());
        assertEquals("token123", dto.token());
        assertEquals("sensei@forge.com", dto.email());
        assertEquals("Sensei Ketan", dto.fullName());
        assertEquals("USER", dto.role());
    }

    @Test
    void testBidRequest() {
        BidRequest dto1 = new BidRequest();
        dto1.setAuctionId(10L);
        dto1.setUserEmail("test@test.com");
        dto1.setAmount(new BigDecimal("500"));
        dto1.setBidAmount(new BigDecimal("550"));

        BidRequest dto2 = new BidRequest();
        dto2.setAuctionId(10L);
        dto2.setUserEmail("test@test.com");
        dto2.setAmount(new BigDecimal("500"));
        dto2.setBidAmount(new BigDecimal("550"));

        assertEquals(10L, dto1.getAuctionId());
        assertEquals("test@test.com", dto1.getUserEmail());
        assertEquals(new BigDecimal("500"), dto1.getAmount());
        assertEquals(new BigDecimal("550"), dto1.getBidAmount());

        assertEquals(dto1, dto2);
        assertNotNull(dto1.hashCode());
        assertNotNull(dto1.toString());
    }

    @Test
    void testBidResponseDto() {
        LocalDateTime now = LocalDateTime.now();
        BidResponseDto dto = new BidResponseDto(1L, BigDecimal.TEN, 2L, "Macbook", "ACTIVE", "imgUrl", true, now, 3L, 3L);

        assertEquals(1L, dto.bidId());
        assertEquals(BigDecimal.TEN, dto.amount());
        assertTrue(dto.successful());
        assertEquals("imgUrl", dto.imageUrl());
    }

    @Test
    void testLoginRequest() {
        LoginRequest dto = new LoginRequest("sensei@forge.com", "Barclays@2026");
        assertEquals("sensei@forge.com", dto.email());
        assertEquals("Barclays@2026", dto.password());
    }

    @Test
    void testUserCreateDto() {
        UserCreateDto dto1 = new UserCreateDto("sensei@forge.com", "pass123", "Sensei Ketan");
        UserCreateDto dto2 = UserCreateDto.builder()
                .email("sensei@forge.com")
                .password("pass123")
                .fullName("Sensei Ketan")
                .build();

        UserCreateDto dto3 = new UserCreateDto();
        dto3.setEmail("diff@forge.com");

        assertEquals("sensei@forge.com", dto1.getEmail());
        assertEquals("pass123", dto1.getPassword());
        assertEquals("Sensei Ketan", dto1.getFullName());

        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertNotNull(dto1.hashCode());
        assertNotNull(dto1.toString());
    }

    @Test
    void testUserRequestDto() {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName("Ketan Rajput");
        dto.setCurrentPassword("oldPass");
        dto.setNewPassword("newPass");

        assertEquals("Ketan Rajput", dto.getFullName());
        assertEquals("oldPass", dto.getCurrentPassword());
        assertEquals("newPass", dto.getNewPassword());
    }

    @Test
    void testUserResponseDto() {
        LocalDateTime now = LocalDateTime.now();
        UserResponseDto dto = new UserResponseDto(100L, "sensei@forge.com", "Sensei Ketan", BigDecimal.valueOf(50000), now);

        assertEquals(100L, dto.id());
        assertEquals("sensei@forge.com", dto.email());
        assertEquals(BigDecimal.valueOf(50000), dto.walletBalance());
        assertNotNull(dto.createdAt());
    }
}