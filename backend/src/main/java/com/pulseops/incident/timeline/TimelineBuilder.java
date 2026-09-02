package com.pulseops.incident.timeline;

import com.pulseops.correlation.model.SignalEvent;
import com.pulseops.deployments.model.DeploymentEntity;
import com.pulseops.incident.model.IncidentEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merges an incident's events, the deployments around it, and its lifecycle
 * milestones into one ordered timeline for the dashboard.
 */
@Component
public class TimelineBuilder {

    public List<TimelineEntry> build(IncidentEntity incident,
                                     List<SignalEvent> events,
                                     List<DeploymentEntity> deployments) {
        List<TimelineEntry> entries = new ArrayList<>();

        for (DeploymentEntity d : deployments) {
            entries.add(new TimelineEntry(
                    d.getDeployedAt(), "DEPLOYMENT",
                    "Deployment to " + d.getService(),
                    "Version " + (d.getVersion() == null ? "n/a" : d.getVersion()),
                    null, d.getPublicId()));
        }

        for (SignalEvent e : events) {
            entries.add(new TimelineEntry(
                    e.occurredAt(), "EVENT",
                    e.eventType() + " on " + e.service(),
                    e.message(),
                    e.severity().name(), e.publicId()));
        }

        entries.add(new TimelineEntry(
                incident.getCreatedAt(), "INCIDENT",
                "Incident " + incident.getPublicId() + " created",
                incident.getCorrelationSummary(),
                incident.getSeverity().name(), incident.getPublicId()));

        if (incident.getResolvedAt() != null) {
            entries.add(new TimelineEntry(
                    incident.getResolvedAt(), "INCIDENT",
                    "Incident " + incident.getPublicId() + " resolved",
                    null, null, incident.getPublicId()));
        }

        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries;
    }
}
