package com.forge.platform.service;

import com.forge.platform.dto.UserRequestDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("sensei@forge.com")
                .password("hashed_password")
                .fullName("Ketansingh Rajput")
                .build();
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Test
    void registerUser_encodesPasswordAndSavesUser() {
        when(passwordEncoder.encode("rawpass")).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(walletRepository.save(any(Wallet.class))).thenReturn(mock(Wallet.class));

        User input = User.builder()
                .email("sensei@forge.com")
                .password("rawpass")
                .fullName("Ketansingh Rajput")
                .build();

        User result = userService.registerUser(input);

        verify(passwordEncoder).encode("rawpass");
        verify(userRepository).save(input);
        verify(walletRepository).save(any(Wallet.class)); // wallet created
        assertNotNull(result);
    }

    @Test
    void registerUser_initializesWalletWithZeroBalance() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(testUser);

        userService.registerUser(testUser);

        verify(walletRepository).save(argThat(wallet ->
                wallet.getTotalBalance().compareTo(BigDecimal.ZERO) == 0 &&
                        wallet.getLockedAmount().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    // ── getUserProfile ────────────────────────────────────────────────────────

    @Test
    void getUserProfile_returnsCorrectDto_withWalletBalance() {
        Wallet wallet = mock(Wallet.class);
        when(wallet.getAvailableBalance()).thenReturn(new BigDecimal("5000.00"));
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(wallet));

        UserResponseDto dto = userService.getUserProfile(testUser);

        assertEquals("sensei@forge.com", dto.email());
        assertEquals("Ketansingh Rajput", dto.fullName());
        assertEquals(new BigDecimal("5000.00"), dto.walletBalance());
    }

    @Test
    void getUserProfile_returnsZeroBalance_whenWalletNotFound() {
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.empty());

        UserResponseDto dto = userService.getUserProfile(testUser);

        assertEquals(BigDecimal.ZERO, dto.walletBalance());
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_updatesFullName_whenProvided() {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName("Sensei Updated");

        Wallet wallet = mock(Wallet.class);
        when(wallet.getTotalBalance()).thenReturn(BigDecimal.ZERO);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(wallet));

        UserResponseDto result = userService.updateProfile(testUser, dto);

        assertEquals("Sensei Updated", testUser.getFullName());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_doesNotChangeName_whenDtoNameIsBlank() {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName("   "); // blank

        Wallet wallet = mock(Wallet.class);
        when(wallet.getTotalBalance()).thenReturn(BigDecimal.ZERO);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(wallet));

        userService.updateProfile(testUser, dto);

        assertEquals("Ketansingh Rajput", testUser.getFullName()); // unchanged
    }

    @Test
    void updateProfile_doesNotChangeName_whenDtoNameIsNull() {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName(null);

        Wallet wallet = mock(Wallet.class);
        when(wallet.getTotalBalance()).thenReturn(BigDecimal.ZERO);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(wallet));

        userService.updateProfile(testUser, dto);

        assertEquals("Ketansingh Rajput", testUser.getFullName());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_success_whenCurrentPasswordMatches() {
        UserRequestDto dto = new UserRequestDto();
        dto.setCurrentPassword("oldpass");
        dto.setNewPassword("newpass123");

        when(passwordEncoder.matches("oldpass", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("new_hashed");
        when(userRepository.save(testUser)).thenReturn(testUser);

        assertDoesNotThrow(() -> userService.changePassword(testUser, dto));
        verify(userRepository).save(testUser);
        assertEquals("new_hashed", testUser.getPassword());
    }

    @Test
    void changePassword_throwsException_whenCurrentPasswordWrong() {
        UserRequestDto dto = new UserRequestDto();
        dto.setCurrentPassword("wrongpass");
        dto.setNewPassword("newpass123");

        when(passwordEncoder.matches("wrongpass", "hashed_password")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.changePassword(testUser, dto));

        assertEquals("Current password incorrect", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // ── getUserByEmail ────────────────────────────────────────────────────────

    @Test
    void getUserByEmail_returnsUser_whenFound() {
        when(userRepository.findByEmail("sensei@forge.com")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("sensei@forge.com");

        assertEquals(testUser, result);
    }

    @Test
    void getUserByEmail_throwsException_whenNotFound() {
        when(userRepository.findByEmail("ghost@forge.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserByEmail("ghost@forge.com"));
    }

    // ── getUserAvailableBalance ───────────────────────────────────────────────

    @Test
    void getUserAvailableBalance_returnsBalance_whenWalletExists() {
        Wallet wallet = mock(Wallet.class);
        when(wallet.getAvailableBalance()).thenReturn(new BigDecimal("2500.00"));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        BigDecimal balance = userService.getUserAvailableBalance(1L);

        assertEquals(new BigDecimal("2500.00"), balance);
    }

    @Test
    void getUserAvailableBalance_returnsZero_whenNoWallet() {
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        BigDecimal balance = userService.getUserAvailableBalance(99L);

        assertEquals(BigDecimal.ZERO, balance);
    }
}