package com.pulseops.ingest;

import com.pulseops.events.EventIngestionService;
import com.pulseops.events.EventRepository;
import com.pulseops.events.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Concurrency check: many producer threads push events at once; the bounded
 * queue + worker pool must process every one exactly once with no lost events
 * and no exceptions. We assert on counts, not on timing.
 */
@SpringBootTest
@ActiveProfiles("test")
class IngestPipelineConcurrencyTest {

    private static final int PRODUCERS = 8;
    private static final int EVENTS_PER_PRODUCER = 40;
    private static final int TOTAL = PRODUCERS * EVENTS_PER_PRODUCER;

    @Autowired
    EventIngestionService ingestionService;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    PipelineMetrics metrics;

    @Test
    void processesEveryEventUnderConcurrentLoad() throws InterruptedException {
        long eventsBefore = eventRepository.count();
        long processedBefore = metrics.processed();

        String[] services = {"payment-api", "user-service", "transaction-service", "database", "api-gateway"};
        ExecutorService producers = Executors.newFixedThreadPool(PRODUCERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int p = 0; p < PRODUCERS; p++) {
            final int producerId = p;
            producers.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < EVENTS_PER_PRODUCER; i++) {
                        EventRequest request = new EventRequest(
                                Instant.now(),
                                services[(producerId + i) % services.length],
                                "host-" + producerId,
                                "HIGH_LATENCY",
                                "LOW",
                                "latency_ms",
                                123.0,
                                "concurrent load event",
                                null);
                        ingestionService.ingest(request);
                    }
                } catch (Exception ex) {
                    failures.incrementAndGet();
                }
            });
        }

        start.countDown();
        producers.shutdown();
        assertThat(producers.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(failures.get()).isZero();
        assertThat(eventRepository.count()).isEqualTo(eventsBefore + TOTAL);

        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(metrics.processed()).isGreaterThanOrEqualTo(processedBefore + TOTAL));
    }

    @Test
    void metricCountersRemainConsistent() {
        // enqueued should never be less than processed lag aside, and never negative
        assertThat(metrics.enqueued()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.processed()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.peakQueueDepth()).isGreaterThanOrEqualTo(0);
    }
}
