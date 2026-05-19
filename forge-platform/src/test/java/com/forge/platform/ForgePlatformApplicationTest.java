package com.forge.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// Requires PostgreSQL + Redis running — disabled for CI/unit test runs
@Disabled("Integration test — needs full infra (PostgreSQL, Redis, MinIO)")
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.main.banner-mode=off"
})
@ActiveProfiles("test")
class ForgePlatformApplicationTest {

    @Test
    void contextLoads() {
        // Isse main class ki lines cover ho jayengi bina original file chhede
        assertDoesNotThrow(() -> {
            ForgePlatformApplication.main(new String[]{
                    "--server.port=0",
                    "--spring.main.allow-bean-definition-overriding=true"
            });
        });
    }
}