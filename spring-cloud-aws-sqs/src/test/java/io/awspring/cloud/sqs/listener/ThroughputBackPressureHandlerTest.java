/*
 * Copyright 2013-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.sqs.listener;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.ThroughputBackPressureHandler;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link ThroughputBackPressureHandler}.
 *
 * @author Loïc Rouchon
 */
class ThroughputBackPressureHandlerTest {
	private ThroughputBackPressureHandler handler;

	@BeforeEach
	void setUp() {
		handler = new ThroughputBackPressureHandler.Builder().batchSize(5).build();
	}

	@ParameterizedTest
	@CsvSource({ "4,4", "5,5", "6,5", })
	void amountIsCappedAtBatchSize(int requestedAmount, int expectedPermits) throws InterruptedException {
		assertThat(handler.request(requestedAmount)).isEqualTo(expectedPermits);
	}

	@ParameterizedTest
	@CsvSource({ "LIMITED,0", "PROCESSED,0", "NONE_FETCHED,5", "PARTIAL_FETCH,5", })
	void lowThroughputMode_shouldReturnZeroUntilRelease(BackPressureHandler.ReleaseReason releaseReason,
			int expectedPermitsAfterRelease) throws InterruptedException {
		// Given a first batch
		int batchSize = 5;
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		// When a second batch is requested, it should return zero permits (because low throughput mode)
		assertThat(handler.request(batchSize)).isZero();
		// When a batch is requested after a release, the expected permits should be
		// returned depending on the release reason
		handler.release(1, releaseReason);
		assertThat(handler.request(batchSize)).isEqualTo(expectedPermitsAfterRelease);
	}

	@Test
	void highThroughputMode_shouldAllowMultipleConcurrentRequests() throws InterruptedException {
		// Given a first batch with polled messages
		int batchSize = 5;
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		handler.release(0, BackPressureHandler.ReleaseReason.PARTIAL_FETCH); // switch to HIGH
		// Then subsequent requests should return the same batch size
		// because we are in high throughput mode
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		handler.release(0, BackPressureHandler.ReleaseReason.PARTIAL_FETCH);
		handler.release(0, BackPressureHandler.ReleaseReason.PARTIAL_FETCH);
		// When a fetch returns no messages, throughput mode should switch to LOW
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		handler.release(5, BackPressureHandler.ReleaseReason.NONE_FETCHED);
		assertThat(handler.request(batchSize)).isEqualTo(batchSize);
		// And subsequent requests should return zero permits until the current batch finishes with NONE_FETCHED
		assertThat(handler.request(batchSize)).isZero();
		assertThat(handler.request(batchSize)).isZero();
		handler.release(5, BackPressureHandler.ReleaseReason.NONE_FETCHED);
		assertThat(handler.request(batchSize)).isEqualTo(5);
		// or until it (the current batch) finishes with PARTIAL_FETCH
		assertThat(handler.request(batchSize)).isZero();
		assertThat(handler.request(batchSize)).isZero();
		handler.release(3, BackPressureHandler.ReleaseReason.PARTIAL_FETCH);
		assertThat(handler.request(batchSize)).isEqualTo(5);
	}

	@Test
	void drain_shouldSetDrainedAndReturnTrue() throws InterruptedException {
		boolean result = handler.drain(Duration.ofSeconds(1));
		assertThat(result).isTrue();
		assertThat(handler.request(5)).isZero();
	}
}
