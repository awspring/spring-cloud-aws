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

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.messaging.Message;

public abstract class BaseExponentialBackoffErrorHandlerJitterTest {
	static class BaseTestCase {
		String sqsApproximateReceiveCount;
		Supplier<Random> randomSupplier;
		int initialVisibilityTimeoutSeconds;
		double multiplier;
		int VisibilityTimeoutExpectedFullJitter;
		int VisibilityTimeoutExpectedHalfJitter;

		BaseTestCase sqsApproximateReceiveCount(String sqsApproximateReceiveCount) {
			this.sqsApproximateReceiveCount = sqsApproximateReceiveCount;
			return this;
		}

		BaseTestCase randomSupplier(Supplier<Random> randomSupplier) {
			this.randomSupplier = randomSupplier;
			return this;
		}

		BaseTestCase initialVisibilityTimeoutSeconds(int initialVisibilityTimeoutSeconds) {
			this.initialVisibilityTimeoutSeconds = initialVisibilityTimeoutSeconds;
			return this;
		}

		BaseTestCase multiplier(double multiplier) {
			this.multiplier = multiplier;
			return this;
		}

		BaseTestCase VisibilityTimeoutExpectedHalfJitter(int VisibilityTimeoutExpectedHalfJitter) {
			this.VisibilityTimeoutExpectedHalfJitter = VisibilityTimeoutExpectedHalfJitter;
			return this;
		}

		BaseTestCase VisibilityTimeoutExpectedFullJitter(int VisibilityTimeoutExpectedFullJitter) {
			this.VisibilityTimeoutExpectedFullJitter = VisibilityTimeoutExpectedFullJitter;
			return this;
		}

		CompletableFuture<Void> calculateWithVisibilityTimeoutExpectedHalfJitter(Message<Object> message, Throwable t) {
			ExponentialBackoffErrorHandler<Object> handler = ExponentialBackoffErrorHandler.builder()
					.randomSupplier(this.randomSupplier)
					.initialVisibilityTimeoutSeconds(this.initialVisibilityTimeoutSeconds).multiplier(this.multiplier)
					.jitter(Jitter.HALF).build();

			return handler.handle(message, t);
		}

		CompletableFuture<Void> calculateWithVisibilityTimeoutExpectedHalfJitter(Collection<Message<Object>> messages,
				Throwable t) {
			ExponentialBackoffErrorHandler<Object> handler = ExponentialBackoffErrorHandler.builder()
					.randomSupplier(this.randomSupplier)
					.initialVisibilityTimeoutSeconds(this.initialVisibilityTimeoutSeconds).multiplier(this.multiplier)
					.jitter(Jitter.HALF).build();

			return handler.handle(messages, t);
		}

		CompletableFuture<Void> calculateWithVisibilityTimeoutExpectedFullJitter(Message<Object> message, Throwable t) {
			ExponentialBackoffErrorHandler<Object> handler = ExponentialBackoffErrorHandler.builder()
					.randomSupplier(this.randomSupplier)
					.initialVisibilityTimeoutSeconds(this.initialVisibilityTimeoutSeconds).multiplier(this.multiplier)
					.jitter(Jitter.FULL).build();

			return handler.handle(message, t);
		}

		CompletableFuture<Void> calculateWithVisibilityTimeoutExpectedFullJitter(Collection<Message<Object>> messages,
				Throwable t) {
			ExponentialBackoffErrorHandler<Object> handler = ExponentialBackoffErrorHandler.builder()
					.randomSupplier(this.randomSupplier)
					.initialVisibilityTimeoutSeconds(this.initialVisibilityTimeoutSeconds).multiplier(this.multiplier)
					.jitter(Jitter.FULL).build();

			return handler.handle(messages, t);
		}
	}

	static class MockedRandomNextInt extends Random {
		final Function<Integer, Integer> nextInt;

		MockedRandomNextInt(Function<Integer, Integer> nextInt) {
			this.nextInt = nextInt;
		}

		@Override
		public int nextInt(int bound) {
			return nextInt.apply(bound);
		}

		@Override
		public int nextInt(int origin, int bound) {
			return nextInt.apply(bound - origin) + origin;
		}
	}
}
