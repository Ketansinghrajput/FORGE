package com.forge.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.platform.dto.WalletTransactionDTO;
import com.forge.platform.entity.User;
import com.forge.platform.enums.TransactionType;
import com.forge.platform.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WalletControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private WalletService walletService;
    @MockBean private com.forge.platform.security.JwtService jwtService;
    @MockBean private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("sensei@forge.com");
        testUser.setFullName("Sensei Rajput");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── GET /api/v1/wallet/balance ────────────────────────────────────────────

    @Test
    void getBalance_returns200_withBalanceMap() throws Exception {
        Map<String, BigDecimal> balance = Map.of(
                "totalBalance", new BigDecimal("10000.00"),
                "lockedAmount", new BigDecimal("2000.00"),
                "availableBalance", new BigDecimal("8000.00")
        );
        when(walletService.getMyBalance("sensei@forge.com")).thenReturn(balance);

        mockMvc.perform(get("/api/v1/wallet/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(8000.00))
                .andExpect(jsonPath("$.totalBalance").value(10000.00))
                .andExpect(jsonPath("$.lockedAmount").value(2000.00));
    }

    // ── POST /api/v1/wallet/topup ─────────────────────────────────────────────

    @Test
    void topUp_returns200_whenAmountValid() throws Exception {
        doNothing().when(walletService).topUpWallet(eq("sensei@forge.com"), any());
        when(walletService.getMyBalance("sensei@forge.com")).thenReturn(
                Map.of("availableBalance", new BigDecimal("6000.00"))
        );

        Map<String, BigDecimal> body = Map.of("amount", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedAmount").value(1000.00))
                .andExpect(jsonPath("$.availableBalance").value(6000.00));

        verify(walletService).topUpWallet("sensei@forge.com", new BigDecimal("1000.00"));
    }

    @Test
    void topUp_returns400_whenAmountIsZero() throws Exception {
        Map<String, BigDecimal> body = Map.of("amount", BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(walletService);
    }

    @Test
    void topUp_returns400_whenAmountIsNegative() throws Exception {
        Map<String, BigDecimal> body = Map.of("amount", new BigDecimal("-500.00"));

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    @Test
    void topUp_returns400_whenAmountMissing() throws Exception {
        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletService);
    }

    @Test
    void topUp_returns500_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(walletService).topUpWallet(anyString(), any());

        Map<String, BigDecimal> body = Map.of("amount", new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── GET /api/v1/wallet/transactions ──────────────────────────────────────

    @Test
    void getTransactions_returns200_withPagedResults() throws Exception {
        WalletTransactionDTO tx = mock(WalletTransactionDTO.class);
        PageImpl<WalletTransactionDTO> page = new PageImpl<>(
                List.of(tx), PageRequest.of(0, 10), 1
        );
        when(walletService.getTransactionHistory(eq("sensei@forge.com"), isNull(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/wallet/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getTransactions_filtersBy_transactionType() throws Exception {
        PageImpl<WalletTransactionDTO> page = new PageImpl<>(List.of());
        when(walletService.getTransactionHistory(eq("sensei@forge.com"), eq(TransactionType.CREDIT), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/wallet/transactions")
                        .param("type", "CREDIT"))
                .andExpect(status().isOk());

        verify(walletService).getTransactionHistory(eq("sensei@forge.com"), eq(TransactionType.CREDIT), any());
    }
}