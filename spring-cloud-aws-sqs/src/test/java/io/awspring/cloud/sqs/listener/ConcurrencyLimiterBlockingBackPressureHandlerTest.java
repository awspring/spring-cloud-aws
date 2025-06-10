package io.awspring.cloud.sqs.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyLimiterBlockingBackPressureHandlerTest {

	private static final int BATCH_SIZE = 5;
	private static final int TOTAL_PERMITS = 10;

	private ConcurrencyLimiterBlockingBackPressureHandler handler;

    @BeforeEach
    void setUp() {
        handler = ConcurrencyLimiterBlockingBackPressureHandler.builder()
                .totalPermits(TOTAL_PERMITS)
                .batchSize(BATCH_SIZE)
                .acquireTimeout(Duration.ofMillis(100))
                .build();
    }

    @Test
    void request_shouldAcquirePermits() throws InterruptedException {
		// Requesting a first batch should acquire the permits
        assertThat(handler.request(BATCH_SIZE)).isEqualTo(BATCH_SIZE);
		// Requesting a second batch should acquire the remaining permits
        assertThat(handler.request(BATCH_SIZE)).isEqualTo(BATCH_SIZE);
        // No permits left
        assertThat(handler.request(1)).isZero();
    }

    @Test
    void release_shouldAllowFurtherRequests() throws InterruptedException {
		// Given all permits are acquired
		assertThat(handler.request(TOTAL_PERMITS)).isEqualTo(TOTAL_PERMITS);
        assertThat(handler.request(1)).isZero();
		// When releasing some permits, new requests should be allowed
        handler.release(3, BackPressureHandler.ReleaseReason.PROCESSED);
		assertThat(handler.request(5)).isEqualTo(3); // Only 3 permits were released so far
    }
}

