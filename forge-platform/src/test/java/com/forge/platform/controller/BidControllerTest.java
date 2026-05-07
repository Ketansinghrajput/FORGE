package com.forge.platform.controller;

import com.forge.platform.dto.BidResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.BidService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BidController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BidService bidService;

    @MockBean
    private com.forge.platform.security.JwtService jwtService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("sensei@forge.com");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(testUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyBids_Success_And_Sorted() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // 🔥 SENSEI FIX: Using the exact 10-arg constructor as required by your class
        // Order: Long id, BigDecimal amount, Long auctionId, String auctionTitle, String bidderEmail, String bidderName, boolean won, LocalDateTime placedAt, Long userId, Long winningBidId

        BidResponseDto oldBid = new BidResponseDto(
                10L, null, null, "Auction A", null, null, false, now.minusHours(2), null, null
        );

        BidResponseDto newBid = new BidResponseDto(
                11L, null, null, "Auction B", null, null, false, now.minusMinutes(10), null, null
        );

        BidResponseDto nullDateBid = new BidResponseDto(
                12L, null, null, "Auction C", null, null, false, null, null, null
        );

        when(bidService.getMyBids(1L)).thenReturn(java.util.Arrays.asList(oldBid, newBid, nullDateBid));

        mockMvc.perform(get("/api/v1/bids/my")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 🔥 SENSEI FIX: Changed "id" to "bidId" to match the JSON response
                .andExpect(jsonPath("$[0].bidId").value(11))
                .andExpect(jsonPath("$[1].bidId").value(10))
                .andExpect(jsonPath("$[2].bidId").value(12));

        verify(bidService).getMyBids(1L);
    }
}