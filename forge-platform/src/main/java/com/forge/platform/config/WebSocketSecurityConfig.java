package com.forge.platform.config;

import com.forge.platform.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
                        try {
                            String userEmail = jwtService.extractUsername(token);

                            if (userEmail != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                                if (jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());

                                    // 🚀 SENSEI: Ye line sabse important hai!
                                    // Isse Spring Message mapping ko pata chalta hai user kaun hai.
                                    accessor.setUser(authToken);
                                    log.info("🟢 STOMP Auth Success: User [{}] connected", userEmail);
                                } else {
                                    log.error("🔴 STOMP Auth Failed: Token invalid for user {}", userEmail);
                                    throw new IllegalArgumentException("Invalid Token"); // Connection reject karne ke liye
                                }
                            }
                        } catch (Exception e) {
                            log.error("🔴 STOMP Token Error: {}", e.getMessage());
                            throw new IllegalArgumentException("Auth Error: " + e.getMessage());
                        }
                    } else {
                        log.warn("🔴 STOMP Auth Failed: Token literally 'null' or empty");
                        // Authentication fail hone par connection allow nahi karna chahiye
                        throw new IllegalArgumentException("No Token Found");
                    }
                }
                return message;
            }
        });
    }
}