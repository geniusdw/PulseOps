package com.pulseops.dashboard;

import com.pulseops.dashboard.api.DashboardOverview;
import com.pulseops.dashboard.api.DashboardOverview.PipelineStatus;
import com.pulseops.dashboard.api.DashboardOverview.TimeBucket;
import com.pulseops.events.EventRepository;
import com.pulseops.ingest.IngestProperties;
import com.pulseops.ingest.IngestQueue;
import com.pulseops.ingest.PipelineMetrics;
import com.pulseops.incident.IncidentEventLinkRepository;
import com.pulseops.incident.IncidentQueryService;
import com.pulseops.incident.IncidentRepository;
import com.pulseops.incident.model.IncidentEntity;
import com.pulseops.incident.model.IncidentEventLink;
import com.pulseops.incident.model.IncidentStatus;
import com.pulseops.events.model.EventEntity;
import com.pulseops.events.model.Severity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Aggregates the numbers shown on the dashboard landing page. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int VOLUME_WINDOW_MINUTES = 30;
    private static final int RATE_WINDOW_MINUTES = 15;

    private final EventRepository eventRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventLinkRepository linkRepository;
    private final IncidentQueryService incidentQueryService;
    private final PipelineMetrics metrics;
    private final IngestQueue queue;
    private final IngestProperties ingestProperties;

    public DashboardService(EventRepository eventRepository,
                            IncidentRepository incidentRepository,
                            IncidentEventLinkRepository linkRepository,
                            IncidentQueryService incidentQueryService,
                            PipelineMetrics metrics,
                            IngestQueue queue,
                            IngestProperties ingestProperties) {
        this.eventRepository = eventRepository;
        this.incidentRepository = incidentRepository;
        this.linkRepository = linkRepository;
        this.incidentQueryService = incidentQueryService;
        this.metrics = metrics;
        this.queue = queue;
        this.ingestProperties = ingestProperties;
    }

    public DashboardOverview overview() {
        Instant now = Instant.now();

        long activeIncidents = incidentRepository.countByStatusNot(IncidentStatus.RESOLVED);
        List<IncidentEntity> openIncidents = incidentRepository.findByStatusNot(IncidentStatus.RESOLVED);
        long criticalIncidents = openIncidents.stream()
                .filter(i -> i.getSeverity() == Severity.CRITICAL)
                .count();

        TreeSet<String> affectedServices = new TreeSet<>();
        if (!openIncidents.isEmpty()) {
            List<Long> ids = openIncidents.stream().map(IncidentEntity::getId).toList();
            List<Long> eventIds = linkRepository.findByIncidentIdIn(ids).stream()
                    .map(IncidentEventLink::getEventId).toList();
            eventRepository.findAllById(eventIds).stream()
                    .map(EventEntity::getService)
                    .forEach(affectedServices::add);
        }

        long eventsInRateWindow = eventRepository.countByOccurredAtBetween(
                now.minus(RATE_WINDOW_MINUTES, ChronoUnit.MINUTES), now);
        double eventsPerMinute = Math.round((eventsInRateWindow / (double) RATE_WINDOW_MINUTES) * 10.0) / 10.0;

        List<TimeBucket> volume = eventVolume(now);

        Map<String, Long> severityDistribution = severityDistribution(openIncidents);

        var recentIncidents = incidentQueryService.list(null, null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"))).content();

        PipelineStatus pipeline = new PipelineStatus(
                queue.size(),
                ingestProperties.queueCapacity(),
                metrics.peakQueueDepth(),
                metrics.enqueued(),
                metrics.processed(),
                metrics.rejected(),
                metrics.failed(),
                ingestProperties.workerCount());

        return new DashboardOverview(
                activeIncidents,
                criticalIncidents,
                eventsPerMinute,
                eventsInRateWindow,
                List.copyOf(affectedServices),
                recentIncidents,
                volume,
                severityDistribution,
                pipeline);
    }

    private List<TimeBucket> eventVolume(Instant now) {
        Instant from = now.minus(VOLUME_WINDOW_MINUTES, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES);
        List<Instant> timestamps = eventRepository.findOccurredAtBetween(from, now);

        long[] counts = new long[VOLUME_WINDOW_MINUTES];
        for (Instant ts : timestamps) {
            int bucket = (int) Duration.between(from, ts).toMinutes();
            if (bucket >= 0 && bucket < counts.length) {
                counts[bucket]++;
            }
        }
        List<TimeBucket> buckets = new ArrayList<>(VOLUME_WINDOW_MINUTES);
        for (int i = 0; i < counts.length; i++) {
            buckets.add(new TimeBucket(from.plus(i, ChronoUnit.MINUTES), counts[i]));
        }
        return buckets;
    }

    private Map<String, Long> severityDistribution(List<IncidentEntity> incidents) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Severity s : Severity.values()) {
            distribution.put(s.name(), 0L);
        }
        for (IncidentEntity incident : incidents) {
            distribution.merge(incident.getSeverity().name(), 1L, Long::sum);
        }
        return distribution;
    }
}
