package com.forge.platform.security; // Make sure ye package main application ke andar ho!

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // POST requests ke liye zaroori
                .cors(AbstractHttpConfigurer::disable) // Cross-origin issues hatane ke liye
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 🔴 TEMPORARY: Saari APIs open kar di hain!
                );
        return http.build();
    }
}