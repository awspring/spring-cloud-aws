package io.awspring.cloud.sqs.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullBatchBackPressureHandlerTest {

	private FullBatchBackPressureHandler handler;

	private final int batchSize = 10;

    @BeforeEach
    void setUp() {
        handler = FullBatchBackPressureHandler.builder()
                .batchSize(batchSize)
                .build();
    }

    @Test
    void request_withExactBatchSize_shouldReturnBatchSize() throws InterruptedException {
        assertThat(handler.request(batchSize)).isEqualTo(batchSize);
    }

    @Test
    void request_withNonBatchSize_shouldReturnZero() throws InterruptedException {
        int permits = handler.request(batchSize - 1);
        assertThat(permits).isZero();
        permits = handler.request(batchSize + 1);
        assertThat(permits).isZero();
    }

    @Test
    void requestBatch_shouldReturnBatchSize() throws InterruptedException {
        assertThat(handler.requestBatch()).isEqualTo(batchSize);
    }
}

