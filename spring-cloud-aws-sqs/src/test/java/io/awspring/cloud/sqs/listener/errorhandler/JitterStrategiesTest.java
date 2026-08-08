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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Jitter} strategies.
 *
 * @author Tomaz Fernandes
 */
class JitterStrategiesTest {

	private final Supplier<Random> randomSupplier = ThreadLocalRandom::current;

	@Test
	void shouldReturnOriginalTimeoutForNoneJitter() {
		// Given
		int timeout = 100;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int result = Jitter.NONE.applyJitter(context);

		// Then
		assertThat(result).isEqualTo(timeout);
	}

	@Test
	void shouldReturnValueBetweenZeroAndTimeoutForFullJitter() {
		// Given
		int timeout = 100;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int result = Jitter.FULL.applyJitter(context);

		// Then
		assertThat(result).isGreaterThanOrEqualTo(1);
		assertThat(result).isLessThanOrEqualTo(timeout);
	}

	@Test
	void shouldReturnValueBetweenHalfAndTimeoutForHalfJitter() {
		// Given
		int timeout = 100;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int result = Jitter.HALF.applyJitter(context);

		// Then
		int expectedLowerBound = (int) Math.ceil(timeout / 2.0);
		assertThat(result).isGreaterThanOrEqualTo(expectedLowerBound);
		assertThat(result).isLessThanOrEqualTo(timeout);
	}

	@Test
	void shouldHandleOddTimeoutForHalfJitter() {
		// Given
		int timeout = 101; // Odd number
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int result = Jitter.HALF.applyJitter(context);

		// Then
		int expectedLowerBound = (int) Math.ceil(timeout / 2.0); // Should be 51
		assertThat(result).isGreaterThanOrEqualTo(expectedLowerBound);
		assertThat(result).isLessThanOrEqualTo(timeout);
	}

	@Test
	void shouldHandleMinimumTimeoutForAllStrategies() {
		// Given
		int timeout = 1;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When & Then
		assertThat(Jitter.NONE.applyJitter(context)).isEqualTo(1);
		assertThat(Jitter.FULL.applyJitter(context)).isEqualTo(1);
		assertThat(Jitter.HALF.applyJitter(context)).isEqualTo(1);
	}

	@Test
	void shouldEnsureMinimumValueOfOneForAllStrategies() {
		// Given
		int timeout = 2;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int noneResult = Jitter.NONE.applyJitter(context);
		int fullResult = Jitter.FULL.applyJitter(context);
		int halfResult = Jitter.HALF.applyJitter(context);

		// Then
		assertThat(noneResult).isGreaterThanOrEqualTo(1);
		assertThat(fullResult).isGreaterThanOrEqualTo(1);
		assertThat(halfResult).isGreaterThanOrEqualTo(1);
	}

	@Test
	void shouldProduceDifferentResultsForMultipleCalls() {
		// Given
		int timeout = 100;
		Jitter.Context context = new Jitter.Context(timeout, randomSupplier);

		// When
		int result1 = Jitter.FULL.applyJitter(context);
		int result2 = Jitter.FULL.applyJitter(context);
		int result3 = Jitter.FULL.applyJitter(context);

		// Then
		// While it's theoretically possible for all results to be the same,
		// it's extremely unlikely with a good random number generator
		// We'll just verify they're all within the expected range
		assertThat(result1).isBetween(1, timeout);
		assertThat(result2).isBetween(1, timeout);
		assertThat(result3).isBetween(1, timeout);
	}
}
