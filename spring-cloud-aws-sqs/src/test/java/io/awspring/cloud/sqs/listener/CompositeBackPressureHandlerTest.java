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
import static org.mockito.Mockito.*;

import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import io.awspring.cloud.sqs.listener.backpressure.CompositeBackPressureHandler;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompositeBackPressureHandler}.
 *
 * @author Loïc Rouchon
 */
class CompositeBackPressureHandlerTest {

	private BackPressureHandler handler1;
	private BackPressureHandler handler2;

	@BeforeEach
	void setUp() {
		handler1 = mock(BackPressureHandler.class);
		handler2 = mock(BackPressureHandler.class);
	}

	@Test
	void request_shouldDelegateToHandlersAndReturnMinPermits() throws InterruptedException {
		// given
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofSeconds(30)).backPressureHandlers(List.of(handler1, handler2))
				.build();
		when(handler1.request(5)).thenReturn(5);
		when(handler2.request(5)).thenReturn(3);
		// when
		int permits = compositeHandler.request(5);
		// then
		assertThat(permits).isEqualTo(3);
		verify(handler1).request(5);
		verify(handler2).request(5);
	}

	@Test
	void release_shouldDelegateToHandlers() {
		// given
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofSeconds(30)).backPressureHandlers(List.of(handler1, handler2))
				.build();
		// when
		compositeHandler.release(2, BackPressureHandler.ReleaseReason.PROCESSED);
		// then
		verify(handler1).release(2, BackPressureHandler.ReleaseReason.PROCESSED);
		verify(handler2).release(2, BackPressureHandler.ReleaseReason.PROCESSED);
	}

	@Test
	void request_shouldWaitIfNoPermitsAndTimeout() throws InterruptedException {
		// given
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofSeconds(5)).backPressureHandlers(List.of(handler1, handler2))
				.build();
		when(handler1.request(5)).thenReturn(0);
		when(handler2.request(5)).thenReturn(0);
		// when
		long start = System.nanoTime();
		int permits = compositeHandler.request(5);
		Duration duration = Duration.ofNanos(System.nanoTime() - start);
		// then
		assertThat(permits).isZero();
		assertThat(duration).isGreaterThanOrEqualTo(Duration.ofSeconds(1L));
	}

	@Test
	void request_shouldPassReducedPermitsToSubsequentHandlers() throws InterruptedException {
		// given
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofSeconds(30)).backPressureHandlers(List.of(handler1, handler2))
				.build();
		when(handler1.request(10)).thenReturn(5);
		when(handler2.request(5)).thenReturn(5);
		// when
		int permits = compositeHandler.request(10);
		// then
		assertThat(permits).isEqualTo(5);
		verify(handler1).request(10);
		verify(handler2).request(5);
	}

	@Test
	void request_whenLaterHandlerReturnsLessPermits_shouldReleaseDiffWithLimitedOnPreviousHandlers()
			throws InterruptedException {
		// given
		BackPressureHandler handler3 = mock(BackPressureHandler.class);
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofMillis(50))
				.backPressureHandlers(List.of(handler1, handler2, handler3)).build();
		when(handler1.request(5)).thenReturn(4);
		when(handler2.request(4)).thenReturn(2);
		when(handler3.request(2)).thenReturn(1);
		// when
		int permits = compositeHandler.request(5);
		// then
		assertThat(permits).isEqualTo(1);
		verify(handler1).request(5);
		verify(handler2).request(4);
		verify(handler3).request(2);
		verify(handler1).release(3, BackPressureHandler.ReleaseReason.LIMITED);
		verify(handler2).release(1, BackPressureHandler.ReleaseReason.LIMITED);
		verify(handler3, never()).release(anyInt(), any());
	}

	@Test
	void request_shouldUnblockWhenPermitsAreReleased() throws InterruptedException {
		// given
		CompositeBackPressureHandler compositeHandler = compositeHandlerBuilder()
				.noPermitsReturnedWaitTimeout(Duration.ofSeconds(30)).backPressureHandlers(List.of(handler1, handler2))
				.build();
		when(handler1.request(5)).thenReturn(0, 5);
		when(handler2.request(5)).thenReturn(5);

		AtomicInteger result = new AtomicInteger(-1);
		Thread requester = new Thread(() -> {
			try {
				// when
				result.set(compositeHandler.request(5));
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		requester.start();
		Thread.sleep(200); // Ensure requester is waiting
		assertThat(requester.isAlive()).isTrue();
		// when
		compositeHandler.release(5, BackPressureHandler.ReleaseReason.PROCESSED);
		requester.join(2000);
		// then
		assertThat(requester.isAlive()).isFalse();
		assertThat(result.get()).isZero();
		assertThat(compositeHandler.request(5)).isEqualTo(5);
	}

	private static CompositeBackPressureHandler.@NotNull Builder compositeHandlerBuilder() {
		return CompositeBackPressureHandler.builder().batchSize(5);
	}
}
