package com.pulseops.incident.api;

import com.pulseops.incident.rootcause.RootCauseCandidate;

import java.util.List;

public record RootCauseDto(String type, String label, double score, List<String> evidence) {

    public static RootCauseDto from(RootCauseCandidate c) {
        return new RootCauseDto(c.type().name(), c.label(), round(c.score()), c.evidence());
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
