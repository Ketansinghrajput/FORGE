package com.forge.platform.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private MessageBrokerRegistry brokerRegistry;

    @Mock
    private StompEndpointRegistry endpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration registration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Test
    void configureMessageBroker_ShouldEnableSimpleBrokerAndPrefix() {
        // Act
        webSocketConfig.configureMessageBroker(brokerRegistry);

        // Assert
        verify(brokerRegistry, times(1)).enableSimpleBroker("/topic");
        verify(brokerRegistry, times(1)).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_ShouldSetEndpointAndOrigins() {
        // Setup: Mocking the fluent API of StompEndpointRegistry
        when(endpointRegistry.addEndpoint("/ws-forge")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);
        when(registration.setAllowedOrigins(anyString())).thenReturn(registration);

        // Act
        webSocketConfig.registerStompEndpoints(endpointRegistry);

        // Assert
        verify(endpointRegistry).addEndpoint("/ws-forge");
        verify(registration).setAllowedOriginPatterns("*");
        verify(registration).setAllowedOrigins("http://localhost:4200");
        verify(registration).withSockJS();
    }
}