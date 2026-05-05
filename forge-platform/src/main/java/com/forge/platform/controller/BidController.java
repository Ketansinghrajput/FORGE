package com.forge.platform.controller;

import com.forge.platform.entity.Bid;
import com.forge.platform.entity.User;
import com.forge.platform.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidRepository bidRepository;

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getMyBids(@AuthenticationPrincipal User user) {
        List<Bid> bids = bidRepository.findByBidderId(user.getId());

        List<Map<String, Object>> response = bids.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(bid -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bidId",         bid.getId());
                    map.put("amount",        bid.getAmount());
                    map.put("auctionId",     bid.getAuction().getId());
                    map.put("auctionTitle",  bid.getAuction().getTitle());
                    map.put("auctionStatus", bid.getAuction().getStatus().name());
                    map.put("imageUrl",      bid.getAuction().getImageUrl() != null
                            ? bid.getAuction().getImageUrl() : "");
                    map.put("successful",    bid.isSuccessful());
                    map.put("placedAt",      bid.getCreatedAt() != null
                            ? bid.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}