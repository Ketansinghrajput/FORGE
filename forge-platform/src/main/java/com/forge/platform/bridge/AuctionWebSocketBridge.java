package com.forge.platform.bridge;

import com.forge.engine.event.BidPlacedEvent;
import com.forge.engine.event.EventBus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuctionWebSocketBridge {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messagingTemplate;

    public AuctionWebSocketBridge(EventBus eventBus, SimpMessagingTemplate messagingTemplate) {
        this.eventBus = eventBus;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void bridgeEngineToWeb() {
        // Naye EventBus mein sirf event listener pass hota hai
        eventBus.subscribe(event -> {
            // Pattern Matching: Agar event 'BidPlacedEvent' type ka hai toh cast bhi khud ho jayega
            if (event instanceof BidPlacedEvent bidEvent) {
                String destination = "/topic/auctions/" + bidEvent.auctionId();

                // WebSocket broadcast
                messagingTemplate.convertAndSend(destination, bidEvent.bid());

                log.info("⚡ Real-time Broadcast to {}: {} by {}",
                        destination,
                        bidEvent.bid().getPrice().getAmount(),
                        bidEvent.bid().getBidderId()
                );
            }
        });
    }
}