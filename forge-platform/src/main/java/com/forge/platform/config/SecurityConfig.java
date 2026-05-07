package com.forge.platform.config;

import com.forge.platform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer; // SENSEI: Ye import zaroori hai!
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. SENSEI: Ye CORS wala jaadu WebSocket block hone se rokega
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Open Endpoints (Public)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/engine/**",
                                "/ws/**",
                                "/ws-forge/**",// 🔥 SENSEI FIX: Changed /ws-forge/** to /ws/**
                                "/api/v1/auctions/active",  // 🔥 SENSEI FIX: Lobby ko public kiya
                                "/api/v1/images/**",
                                "/error",
                                "/index.html",
                                "/"
                        ).permitAll()

                        // 2. Authenticated Resources (Only logged-in users)
                        .requestMatchers("/api/bids/**").authenticated()
                        .requestMatchers("/api/v1/auctions/**").hasAuthority("ROLE_USER") // Ab bache hue auction routes secure rahenge
                        .requestMatchers("/api/v1/wallet/**").hasAuthority("ROLE_USER")

                        // 3. Fallback Guard
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}