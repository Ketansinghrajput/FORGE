package com.forge.platform.service;

import com.forge.engine.bidding.BiddingEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EngineInitializerTest {

    @Mock private BiddingEngine biddingEngine;
    @InjectMocks private EngineInitializer engineInitializer;

    @Test
    void setupTestAuction_ShouldRegisterAuction() {
        engineInitializer.setupTestAuction();
        verify(biddingEngine).registerAuction(eq(101L), any());
    }
}