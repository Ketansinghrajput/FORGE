package com.forge.platform.bridge;

import com.forge.engine.event.EventBus;
import com.forge.engine.model.Bid;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

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
        eventBus.subscribe("AUCTION_UPDATE", data -> {
            if (data instanceof Bid bid) {
                // Yahan "/1" add kar diya taaki UI ke topic se match ho jaye
                messagingTemplate.convertAndSend("/topic/auctions/1", bid);
                System.out.println("⚡ Real-time Broadcast to /topic/auctions/1: ₹" + bid.getPrice().amount());
            }
        });
    }
}