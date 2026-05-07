package com.forge.platform.scheduler;

import com.forge.platform.service.AuctionManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionManagerService auctionManager;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    @Test
    void processAuctionTransitions_ShouldInvokeManagerMethods() {
        // Act: Scheduler method ko manually call kar rahe hain
        auctionScheduler.processAuctionTransitions();

        // Assert: Verify kar rahe hain ki dono service methods call huye ya nahi
        verify(auctionManager, times(1)).startPendingAuctions();
        verify(auctionManager, times(1)).closeExpiredAuctions();
    }
}