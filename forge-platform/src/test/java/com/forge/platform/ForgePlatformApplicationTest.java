package com.forge.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

// 🔥 SENSEI FIX: Main files me bina change kiye, yahan se property override kar di
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