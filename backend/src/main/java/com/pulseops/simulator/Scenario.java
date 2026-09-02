package com.pulseops.simulator;

import java.util.Arrays;
import java.util.Optional;

/** The failure scenarios the simulator can inject. */
public enum Scenario {

    DATABASE_FAILURE("database-failure",
            "Database connection pool exhaustion cascading to payment failures"),
    BAD_DEPLOYMENT("bad-deployment",
            "A deployment to payment-api followed by errors and a restart"),
    CPU_SATURATION("cpu-saturation",
            "CPU saturation on user-service degrading latency and availability"),
    NETWORK_FAILURE("network-failure",
            "Network errors at the edge causing 503s across services"),
    NORMAL_TRAFFIC("normal-traffic",
            "Low-severity background events that should NOT form an incident");

    private final String slug;
    private final String description;

    Scenario(String slug, String description) {
        this.slug = slug;
        this.description = description;
    }

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
    }

    public static Optional<Scenario> fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(s -> s.slug.equalsIgnoreCase(slug) || s.name().equalsIgnoreCase(slug))
                .findFirst();
    }
}
