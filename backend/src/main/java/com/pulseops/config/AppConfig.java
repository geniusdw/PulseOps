package com.pulseops.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /**
     * A single injectable clock. Production code never calls {@code Instant.now()}
     * directly, so tests can substitute a fixed clock and assert on time-based
     * logic (temporal scoring, deployment lookback) deterministically.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
