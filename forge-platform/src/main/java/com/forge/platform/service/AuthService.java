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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public User register(UserCreateDto request) {
        // FIX 1: Normal class getters use kiye hain
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered!");
        }

        // 1. User save karo
        // 1. User save karo (Bypassing Lombok SuperBuilder Glitch)
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCRYPT
        user.setRole(UserRole.USER);

        user = userRepository.save(user);


        // 2. Wallet banao (10K default balance ke sath)
        Wallet wallet = Wallet.builder()
                .user(user)
                .totalBalance(new BigDecimal("10000.00"))
                .lockedAmount(BigDecimal.ZERO)
                .version(0L)
                .build();

        walletRepository.save(wallet);

        log.info("✅ New user registered with Wallet: {}", user.getEmail());
        return user;
    }

    public AuthResponse login(LoginRequest request) {
        // LoginRequest record hai isliye yahan request.email() chalega
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found after auth!"));

        String jwtToken = jwtService.generateToken(user);

        log.info("✅ User {} logged in successfully. Token generated.", user.getEmail());

        // FIX 2: Check your AuthResponse record!
        // Agar usme 5 fields hain (e.g. userId as String), toh yahan String.valueOf(user.getId()) add karna hoga.
        // Agar message hai toh "Login Successful" daal dena.
        // Niche maine 5 parameters pass kiye hain assuming pehla ID hai:
        return new AuthResponse(
                String.valueOf(user.getId()),  // 1. id
                jwtToken,                      // 2. token
                user.getEmail(),               // 3. email
                user.getFullName(),            // 4. fullName
                user.getRole().name()          // 5. role
        );
    }
}