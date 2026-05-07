package com.forge.platform.bridge;

import com.forge.engine.event.BidPlacedEvent;
import com.forge.engine.event.EventBus;
import com.forge.engine.event.EngineEventListener;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionWebSocketBridgeTest {

    @Mock private EventBus eventBus;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private AuctionWebSocketBridge webSocketBridge;

    @Test
    void bridgeEngineToWeb_ShouldBroadcastWhenBidPlaced() {
        // 1. Capture the EngineEventListener
        ArgumentCaptor<EngineEventListener> captor = ArgumentCaptor.forClass(EngineEventListener.class);
        webSocketBridge.bridgeEngineToWeb();
        verify(eventBus).subscribe(captor.capture());
        EngineEventListener subscriber = captor.getValue();

        // 2. Prepare Mock Event
        Bid mockBid = mock(Bid.class);
        Money mockMoney = mock(Money.class);
        when(mockBid.getPrice()).thenReturn(mockMoney);
        when(mockMoney.getAmount()).thenReturn(BigDecimal.valueOf(100));
        when(mockBid.getBidderId()).thenReturn("1");

        BidPlacedEvent event = new BidPlacedEvent(500L, mockBid);

        // 3. Act: Use onEvent() instead of accept()
        subscriber.onEvent(event);

        // 4. Assert
        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/500"), eq(mockBid));
    }
}