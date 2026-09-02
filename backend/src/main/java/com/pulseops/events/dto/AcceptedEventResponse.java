package com.pulseops.events.dto;

/**
 * Returned by {@code POST /api/events} with HTTP 202 Accepted. The event has
 * been persisted and queued for correlation; incident grouping happens
 * asynchronously on a worker thread.
 */
public record AcceptedEventResponse(String eventId, String status, int queueDepth) {
}
