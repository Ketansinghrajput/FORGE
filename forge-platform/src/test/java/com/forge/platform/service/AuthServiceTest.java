package com.forge.platform.service;

import com.forge.platform.dto.AuthResponse;
import com.forge.platform.dto.LoginRequest;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.enums.UserRole;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import com.forge.platform.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletRepository walletRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private UserCreateDto buildCreateDto() {
        UserCreateDto dto = new UserCreateDto();
        dto.setEmail("levi@forge.com");
        dto.setPassword("secret123");
        dto.setFullName("Levi Forge");
        return dto;
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("levi@forge.com");
        user.setFullName("Levi Forge");
        user.setPassword("encoded");
        user.setRole(UserRole.USER);
        return user;
    }

    // --- register ---

    @Test
    void register_happyPath_savesUserAndWallet() {
        when(userRepository.existsByEmail("levi@forge.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(buildUser());

        User result = authService.register(buildCreateDto());

        assertThat(result.getEmail()).isEqualTo("levi@forge.com");
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("levi@forge.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildCreateDto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    // --- login ---

    @Test
    void login_happyPath_returnsTokenAndUserInfo() {
        LoginRequest req = new LoginRequest("levi@forge.com", "secret123");

        when(userRepository.findByEmail("levi@forge.com")).thenReturn(Optional.of(buildUser()));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(req);

        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.email()).isEqualTo("levi@forge.com");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest req = new LoginRequest("levi@forge.com", "wrongpass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFoundAfterAuth_throws() {
        LoginRequest req = new LoginRequest("ghost@forge.com", "secret123");

        when(userRepository.findByEmail("ghost@forge.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}