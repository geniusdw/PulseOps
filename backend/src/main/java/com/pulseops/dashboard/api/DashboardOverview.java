package com.pulseops.dashboard.api;

import com.pulseops.incident.api.IncidentSummaryDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Everything the Dashboard landing page renders, in one response. */
public record DashboardOverview(
        long activeIncidents,
        long criticalIncidents,
        double eventsPerMinute,
        long eventsLast15Minutes,
        List<String> affectedServices,
        List<IncidentSummaryDto> recentIncidents,
        List<TimeBucket> eventVolume,
        Map<String, Long> severityDistribution,
        PipelineStatus pipeline
) {

    public record TimeBucket(Instant minute, long count) {
    }

    /** Live view of the ingest pipeline for the dashboard. */
    public record PipelineStatus(
            int queueDepth,
            int queueCapacity,
            int peakQueueDepth,
            long enqueued,
            long processed,
            long rejected,
            long failed,
            int workerCount
    ) {
    }
}
