package com.forge.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, String> getHealth() {
        return Map.of(
                "status", "UP",
                "message", "Forge Platform is alive, Sensei!",
                "engine", "Connected"
        );
    }
}