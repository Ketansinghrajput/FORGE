package com.forge.platform.controller;

import com.forge.platform.dto.BidResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BidResponseDto>> getMyBids(@AuthenticationPrincipal User user) {
        // 🔥 SENSEI FIX: Service ab seedha DTO list deti hai
        List<BidResponseDto> response = bidService.getMyBids(user.getId());

        // Latest bids top par dikhane ke liye sorting (placedAt record field hai)
        List<BidResponseDto> sortedResponse = response.stream()
                .sorted((a, b) -> {
                    if (a.placedAt() == null) return 1;
                    if (b.placedAt() == null) return -1;
                    return b.placedAt().compareTo(a.placedAt());
                })
                .toList();

        return ResponseEntity.ok(sortedResponse);
    }
}