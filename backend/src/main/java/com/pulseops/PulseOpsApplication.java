package com.pulseops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PulseOps entry point.
 *
 * <p>PulseOps ingests simulated cloud infrastructure events, correlates related
 * events with a deterministic scoring engine, groups them into incidents, ranks
 * probable root causes and exposes everything over REST for a React dashboard.
 *
 * <p>{@code @ConfigurationPropertiesScan} binds the {@code pulseops.*} keys in
 * {@code application.yml} to typed records so the engine has no magic numbers.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PulseOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseOpsApplication.class, args);
    }
}
