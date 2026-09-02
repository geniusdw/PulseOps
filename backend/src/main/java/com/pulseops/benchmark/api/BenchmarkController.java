package com.pulseops.benchmark.api;

import com.pulseops.benchmark.BenchmarkResult;
import com.pulseops.benchmark.BenchmarkService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/benchmark?count=10000&workers=4&windowSize=500}
 *
 * <p>Runs an in-memory correlation-throughput benchmark and returns the measured
 * numbers. Does not touch the database.
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @PostMapping
    public BenchmarkResult run(
            @RequestParam(defaultValue = "10000") int count,
            @RequestParam(defaultValue = "4") int workers,
            @RequestParam(defaultValue = "500") int windowSize) {
        return benchmarkService.run(count, workers, windowSize);
    }
}
