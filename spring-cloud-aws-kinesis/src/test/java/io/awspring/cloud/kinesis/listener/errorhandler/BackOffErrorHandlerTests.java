/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.listener.errorhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.backoff.FixedBackOff;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class BackOffErrorHandlerTests {

	private final Message<String> message = MessageBuilder.withPayload("record").build();

	private final List<Long> sleeps = new ArrayList<>();

	@AfterEach
	void clearInterruptFlag() {
		Thread.interrupted();
	}

	@Test
	@DisplayName("retries with the back-off intervals until the listener succeeds")
	void retriesUntilSuccess() throws Exception {
		int[] invocations = { 0 };
		FailedDelivery failedDelivery = new FailedDelivery(this.message, new RuntimeException("first"), "orders",
				"shard-1", () -> {
					if (++invocations[0] < 2) {
						throw new RuntimeException("retry failed");
					}
				});
		List<Throwable> recovered = new ArrayList<>();

		handler(new FixedBackOff(50, 5), (delivery, lastFailure) -> recovered.add(lastFailure)).handle(failedDelivery);

		assertThat(invocations[0]).isEqualTo(2);
		assertThat(failedDelivery.getAttempts()).isEqualTo(3);
		assertThat(this.sleeps).containsExactly(50L, 50L);
		assertThat(recovered).isEmpty();
	}

	@Test
	@DisplayName("hands the last failure to the recoverer once the back-off stops")
	void recoversWithLastFailureWhenExhausted() throws Exception {
		int[] invocations = { 0 };
		FailedDelivery failedDelivery = new FailedDelivery(this.message, new RuntimeException("original"), "orders",
				"shard-1", () -> {
					throw new IllegalStateException("attempt " + ++invocations[0]);
				});
		List<Throwable> recovered = new ArrayList<>();

		handler(new FixedBackOff(10, 2), (delivery, lastFailure) -> recovered.add(lastFailure)).handle(failedDelivery);

		assertThat(invocations[0]).isEqualTo(2);
		assertThat(recovered).singleElement().satisfies(failure -> assertThat(failure).hasMessage("attempt 2"));
	}

	@Test
	@DisplayName("a delivery that cannot be retried goes to the recoverer straight away")
	void nonRetryableGoesToRecoverer() throws Exception {
		RuntimeException cause = new RuntimeException("conversion failed");
		FailedDelivery failedDelivery = new FailedDelivery(this.message, cause, "orders", "shard-1", null);
		List<Throwable> recovered = new ArrayList<>();

		handler(new FixedBackOff(10, 3), (delivery, lastFailure) -> recovered.add(lastFailure)).handle(failedDelivery);

		assertThat(recovered).containsExactly(cause);
		assertThat(this.sleeps).isEmpty();
	}

	@Test
	@DisplayName("the default recoverer rethrows the last failure so the shard halts")
	void defaultRecovererRethrows() {
		FailedDelivery failedDelivery = new FailedDelivery(this.message, new RuntimeException("original"), "orders",
				"shard-1", () -> {
					throw new IllegalStateException("still failing");
				});
		BackOffErrorHandler handler = new BackOffErrorHandler(new FixedBackOff(0, 1));

		assertThatThrownBy(() -> handler.handle(failedDelivery)).isInstanceOf(IllegalStateException.class)
				.hasMessage("still failing");
	}

	@Test
	@DisplayName("an interrupt while waiting leaves the failure unresolved and keeps the interrupt flag")
	void interruptLeavesFailureUnresolved() {
		FailedDelivery failedDelivery = new FailedDelivery(this.message, new RuntimeException("original"), "orders",
				"shard-1", () -> {
				});
		List<Throwable> recovered = new ArrayList<>();
		BackOffErrorHandler handler = new BackOffErrorHandler(new FixedBackOff(10, 3),
				(delivery, lastFailure) -> recovered.add(lastFailure)) {
			@Override
			protected void sleep(long intervalMillis) throws InterruptedException {
				throw new InterruptedException("shutting down");
			}
		};

		assertThatThrownBy(() -> handler.handle(failedDelivery)).isInstanceOf(InterruptedException.class);
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		assertThat(failedDelivery.getAttempts()).isEqualTo(1);
		assertThat(recovered).isEmpty();
	}

	private BackOffErrorHandler handler(FixedBackOff backOff, FailedDeliveryRecoverer recoverer) {
		return new BackOffErrorHandler(backOff, recoverer) {
			@Override
			protected void sleep(long intervalMillis) {
				BackOffErrorHandlerTests.this.sleeps.add(intervalMillis);
			}
		};
	}

}
