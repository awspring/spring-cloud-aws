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
package io.awspring.cloud.sqs.listener.errorhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Jitter.Context}.
 *
 * @author Tomaz Fernandes
 */
class JitterContextTest {

	@Test
	void shouldCreateContextWithValidParameters() {
		// Given
		int timeout = 10;
		Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		// When
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// Then
		assertThat(context.getTimeout()).isEqualTo(timeout);
		assertThat(context.getRandomSupplier()).isEqualTo(randomSupplier);
	}

	@Test
	void shouldThrowExceptionWhenTimeoutIsZero() {
		// Given
		int timeout = 0;
		Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		// When & Then
		assertThatThrownBy(() -> new Jitter.Context(timeout, randomSupplier))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Timeout must be >= 1, but was 0");
	}

	@Test
	void shouldThrowExceptionWhenTimeoutIsNegative() {
		// Given
		int timeout = -1;
		Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		// When & Then
		assertThatThrownBy(() -> new Jitter.Context(timeout, randomSupplier))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Timeout must be >= 1, but was -1");
	}

	@Test
	@SuppressWarnings("null")
	void shouldThrowExceptionWhenRandomSupplierIsNull() {
		// Given
		int timeout = 10;
		Supplier<Random> nullSupplier = null;

		// When & Then
		assertThatThrownBy(() -> new Jitter.Context(timeout, nullSupplier)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Random supplier cannot be null");
	}

	@Test
	void shouldAcceptMinimumValidTimeout() {
		// Given
		int timeout = 1;
		Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		// When
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// Then
		assertThat(context.getTimeout()).isEqualTo(1);
		assertThat(context.getRandomSupplier()).isEqualTo(randomSupplier);
	}

	@Test
	void shouldAcceptLargeTimeout() {
		// Given
		int timeout = Integer.MAX_VALUE;
		Supplier<Random> randomSupplier = ThreadLocalRandom::current;

		// When
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// Then
		assertThat(context.getTimeout()).isEqualTo(Integer.MAX_VALUE);
		assertThat(context.getRandomSupplier()).isEqualTo(randomSupplier);
	}
}
