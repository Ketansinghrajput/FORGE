package com.forge.platform.controller;

import com.forge.engine.bidding.BiddingEngine;
import com.forge.engine.model.Bid;
import com.forge.platform.dto.BidRequest;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.dto.UserRequestDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.AuctionService;
import com.forge.platform.service.MinioService;
import com.forge.platform.service.UserService;
import com.forge.platform.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterControllerTest {

    @Mock private AuctionService auctionService;
    @Mock private BiddingEngine biddingEngine;
    @Mock private MinioService minioService;
    @Mock private UserService userService;
    @Mock private WalletService walletService;

    @InjectMocks private BiddingWebSocketController biddingWebSocketController;
    @InjectMocks private EngineStateController engineStateController;
    @InjectMocks private EngineTestController engineTestController;
    @InjectMocks private HealthController healthController;
    @InjectMocks private ImageController imageController;
    @InjectMocks private UserController userController;
    @InjectMocks private WalletController walletController;

    @Mock private Principal principal;
    @Mock private MultipartFile file;

    // --- 1. HealthController ---
    @Test
    void testHealthController() {
        Map<String, String> response = healthController.getHealth();
        assertEquals("UP", response.get("status"));
    }

    // --- 2. BiddingWebSocketController ---
    // --- 2. BiddingWebSocketController ---
    @Test
    void testBiddingWebSocketController() {
        BidRequest request = new BidRequest();
        request.setAuctionId(1L);
        request.setBidAmount(BigDecimal.TEN);

        // Branch 1: Null Principal
        biddingWebSocketController.processBidFromClient(request, null);
        // 🔥 SENSEI FIX: anyString() aur explicit class type use kiya to fix ambiguity
        verify(auctionService, never()).placeBid(any(Long.class), anyString(), any(BigDecimal.class));

        // Branch 2: Valid Principal
        when(principal.getName()).thenReturn("sensei@forge.com");
        biddingWebSocketController.processBidFromClient(request, principal);
        verify(auctionService).placeBid(1L, "sensei@forge.com", BigDecimal.TEN);

        // Branch 3: Exception in Service
        // 🔥 SENSEI FIX: Explicit types here too
        doThrow(new RuntimeException("Oops")).when(auctionService)
                .placeBid(any(Long.class), anyString(), any(BigDecimal.class));
        biddingWebSocketController.processBidFromClient(request, principal);
    }

    // --- 3. EngineStateController ---
    @Test
    void testEngineStateController() {
        when(principal.getName()).thenReturn("sensei@forge.com");
        when(auctionService.getInitialAuctionState(1L, "sensei@forge.com")).thenReturn(Map.of("state", "ACTIVE"));

        ResponseEntity<Map<String, Object>> response = engineStateController.getInitialState(1L, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ACTIVE", response.getBody().get("state"));
    }

    // --- 4. EngineTestController ---
    @Test
    void testEngineTestController() throws Exception {
        when(biddingEngine.placeBid(eq(1L), any(Bid.class))).thenReturn(CompletableFuture.completedFuture(true));
        CompletableFuture<String> result = engineTestController.placeBid(1L, "Sensei", 15000.0);
        assertEquals("ACCEPTED ✅", result.get());
    }

    // --- 5. ImageController ---
    @Test
    void testImageController() {
        // Branch 1: Empty file
        when(file.isEmpty()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> imageController.uploadImage(file));

        // Branch 2: Valid file
        when(file.isEmpty()).thenReturn(false);
        when(minioService.uploadImage(file)).thenReturn("http://minio/image.png");
        ResponseEntity<?> response = imageController.uploadImage(file);
        assertEquals("http://minio/image.png", ((Map<?, ?>) response.getBody()).get("url"));
    }

    // --- 6. UserController ---
    @Test
    void testUserController() {
        User user = new User();
        UserCreateDto createDto = new UserCreateDto("test@test.com", "pass", "Test");
        UserRequestDto reqDto = new UserRequestDto();
        UserResponseDto resDto = new UserResponseDto(1L, "test@test.com", "Test", BigDecimal.ZERO, LocalDateTime.now());

        when(userService.registerUser(any(User.class))).thenReturn(user);
        assertEquals(HttpStatus.OK, userController.register(createDto).getStatusCode());

        when(userService.getUserProfile(user)).thenReturn(resDto);
        assertEquals(HttpStatus.OK, userController.getProfile(user).getStatusCode());

        when(userService.updateProfile(user, reqDto)).thenReturn(resDto);
        assertEquals(HttpStatus.OK, userController.updateProfile(user, reqDto).getStatusCode());

        assertEquals(HttpStatus.OK, userController.changePassword(user, reqDto).getStatusCode());
        verify(userService).changePassword(user, reqDto);
    }

    // --- 7. WalletController ---
    @Test
    void testWalletController() {
        User user = new User();
        user.setEmail("sensei@forge.com");

        // Get Balance
        when(walletService.getMyBalance("sensei@forge.com")).thenReturn(Map.of("balance", BigDecimal.TEN));
        assertEquals(BigDecimal.TEN, walletController.getBalance(user).getBody().get("balance"));

        // TopUp - Invalid amount
        ResponseEntity<?> badResponse = walletController.topUp(Map.of("amount", BigDecimal.ZERO), user);
        assertEquals(HttpStatus.BAD_REQUEST, badResponse.getStatusCode());

        // TopUp - Valid amount
        ResponseEntity<?> okResponse = walletController.topUp(Map.of("amount", BigDecimal.valueOf(500)), user);
        assertEquals(HttpStatus.OK, okResponse.getStatusCode());
        verify(walletService).topUpWallet("sensei@forge.com", BigDecimal.valueOf(500));

        // TopUp - Exception
        doThrow(new RuntimeException("DB Error")).when(walletService).topUpWallet(any(), any());
        ResponseEntity<?> errorResponse = walletController.topUp(Map.of("amount", BigDecimal.valueOf(500)), user);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getStatusCode());
    }
}