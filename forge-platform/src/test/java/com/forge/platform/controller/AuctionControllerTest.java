package com.forge.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.platform.dto.AuctionRequest;
import com.forge.platform.dto.BidRequest;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.enums.AuctionStatus;
import com.forge.platform.service.AuctionService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuctionController.class)
@AutoConfigureMockMvc(addFilters = false) // Filters disabled
@ActiveProfiles("test")
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuctionService auctionService;

    @MockBean
    private com.forge.platform.security.JwtService jwtService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    private User testUser;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("sensei@forge.com");
        testUser.setFullName("Sensei Rajput");

        authPrincipal = new UsernamePasswordAuthenticationToken(testUser, null, List.of());

        // 🔥 THE FIX: Directly forcing the user into the Security Context.
        // Ab @AuthenticationPrincipal isko hi uthayega, chahe filters bypass ho ya nahi.
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @AfterEach
    void tearDown() {
        // Har test ke baad clear karo taaki Unauthenticated wale tests fail na ho
        SecurityContextHolder.clearContext();
    }

    @Test
    void placeBid_Success() throws Exception {
        String jsonRequest = "{\"bidAmount\": 500.00}";

        doNothing().when(auctionService).placeBid(eq(1L), any(User.class), any(BigDecimal.class));

        mockMvc.perform(post("/api/v1/auctions/1/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(content().string("Bid placed successfully by Sensei Rajput"));

        verify(auctionService).placeBid(eq(1L), any(User.class), any(BigDecimal.class));
    }

    @Test
    void placeBid_Unauthenticated_Returns403() throws Exception {
        SecurityContextHolder.clearContext(); // Specifically making it unauthenticated

        String jsonRequest = "{\"bidAmount\": 500.00}";

        mockMvc.perform(post("/api/v1/auctions/1/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User not authenticated"));

        verifyNoInteractions(auctionService);
    }

    @Test
    void getActiveAuctions_Success() throws Exception {
        when(auctionService.getActiveAuctionsPaginated(0, 10)).thenReturn(java.util.Map.of());

        mockMvc.perform(get("/api/v1/auctions/active")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(auctionService).getActiveAuctionsPaginated(0, 10);
    }

    @Test
    void createAuction_Success() throws Exception {
        String jsonRequest = "{\"title\": \"Vintage Rolex\", \"startingPrice\": 1000.00}";

        Auction mockAuction = mock(Auction.class);
        when(mockAuction.getId()).thenReturn(100L);
        when(mockAuction.getTitle()).thenReturn("Vintage Rolex");
        when(mockAuction.getStartingPrice()).thenReturn(BigDecimal.valueOf(1000));
        when(mockAuction.getStartTime()).thenReturn(LocalDateTime.now());
        when(mockAuction.getEndTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(mockAuction.getStatus()).thenReturn(AuctionStatus.ACTIVE);

        when(auctionService.createAuction(any(AuctionRequest.class), any(User.class))).thenReturn(mockAuction);

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.title").value("Vintage Rolex"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(auctionService).createAuction(any(AuctionRequest.class), any(User.class));
    }

    @Test
    void createAuction_Unauthenticated_Returns403() throws Exception {
        SecurityContextHolder.clearContext();

        String jsonRequest = "{\"title\": \"Vintage Rolex\"}";

        mockMvc.perform(post("/api/v1/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User not authenticated"));

        verifyNoInteractions(auctionService);
    }

    @Test
    void deleteAuction_Success() throws Exception {
        doNothing().when(auctionService).deleteAuction(eq(1L), any(User.class));

        mockMvc.perform(delete("/api/v1/auctions/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auction deleted"));

        verify(auctionService).deleteAuction(eq(1L), any(User.class));
    }

    @Test
    void deleteAuction_Unauthenticated_Returns403() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(delete("/api/v1/auctions/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Not authenticated"));

        verifyNoInteractions(auctionService);
    }

    @Test
    void deleteAuction_ThrowsException() throws Exception {
        doThrow(new RuntimeException("Unauthorized to delete")).when(auctionService).deleteAuction(eq(1L), any(User.class));

        mockMvc.perform(delete("/api/v1/auctions/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Unauthorized to delete"));
    }
}