package com.forge.platform.config;

import com.forge.platform.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // SENSEI: Logging zaroori hai production debugging ke liye
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        // SENSEI: Frontend se string "null" aane ka lafda yahan pakdenge
                        if (token.equals("null") || token.trim().isEmpty()) {
                            log.error("🔴 STOMP Auth Failed: Token is literally 'null' or empty");
                            return message;
                        }

                        try {
                            String userEmail = jwtService.extractUsername(token);

                            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                                if (jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());

                                    // Ye line sabse important hai, isi se @AuthenticationPrincipal fill hoga
                                    accessor.setUser(authToken);
                                    log.info("🟢 STOMP Auth Success for user: {}", userEmail);
                                } else {
                                    log.warn("🔴 STOMP Auth Failed: Token is invalid for user {}", userEmail);
                                }
                            }
                        } catch (Exception e) {
                            // SENSEI: Ye line exact exception degi (ExpiredJwtException, MalformedJwtException etc)
                            log.error("🔴 STOMP Token Parsing Error: {}", e.getMessage());
                        }
                    } else {
                        log.warn("🔴 STOMP Auth Failed: No Bearer token found in headers");
                    }
                }
                return message;
            }
        });
    }
}