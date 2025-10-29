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

import io.awspring.cloud.sqs.listener.backpressure.FullBatchBackPressureHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FullBatchBackPressureHandler}.
 *
 * @author Loïc Rouchon
 */
class FullBatchBackPressureHandlerTest {

	private FullBatchBackPressureHandler handler;

	private final int batchSize = 10;

	@BeforeEach
	void setUp() {
		handler = FullBatchBackPressureHandler.builder().batchSize(batchSize).build();
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
