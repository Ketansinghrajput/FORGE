package com.forge.platform.controller;

import com.forge.platform.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class EngineStateController {

    private final AuctionService auctionService;

    @GetMapping("/auction-state/{auctionId}")
    public ResponseEntity<Map<String, Object>> getInitialState(
            @PathVariable Long auctionId,
            Principal principal
    ) {
        Map<String, Object> response = auctionService.getInitialAuctionState(auctionId, principal.getName());
        return ResponseEntity.ok(response);
    }
}