package com.forge.platform.bridge;

import com.forge.engine.event.BidPlacedEvent;
import com.forge.engine.event.EventBus;
import com.forge.engine.event.EngineEventListener;
import com.forge.engine.model.Bid;
import com.forge.engine.model.Money;
import com.forge.platform.entity.Auction;
import com.forge.platform.entity.User;
import com.forge.platform.repository.BidRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseBridgeTest {

    @Mock private EventBus eventBus;
    @Mock private BidRepository bidRepository;
    @Mock private EntityManager entityManager;
    @InjectMocks private DatabaseBridge databaseBridge;

    @Test
    void bridgeEngineToDatabase_ShouldPersistBid() {
        // 1. Capture EngineEventListener
        ArgumentCaptor<EngineEventListener> captor = ArgumentCaptor.forClass(EngineEventListener.class);
        databaseBridge.bridgeEngineToDatabase();
        verify(eventBus).subscribe(captor.capture());
        EngineEventListener subscriber = captor.getValue();

        // 2. Setup Mock Data
        Bid engineBid = mock(Bid.class);
        Money money = mock(Money.class);
        when(engineBid.getPrice()).thenReturn(money);
        when(money.getAmount()).thenReturn(BigDecimal.valueOf(250));
        when(engineBid.getBidderId()).thenReturn("1");

        BidPlacedEvent event = new BidPlacedEvent(100L, engineBid);

        // JPA Reference Mocks
        when(entityManager.getReference(eq(Auction.class), any())).thenReturn(new Auction());
        when(entityManager.getReference(eq(User.class), any())).thenReturn(new User());

        // 3. Act: Trigger listener
        subscriber.onEvent(event);

        // 4. Assert
        verify(bidRepository).save(any(com.forge.platform.entity.Bid.class));
    }
}