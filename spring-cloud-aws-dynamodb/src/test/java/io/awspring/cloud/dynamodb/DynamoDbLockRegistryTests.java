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
package io.awspring.cloud.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class DynamoDbLockRegistryTests implements LocalstackContainerTest {

	private static DynamoDbAsyncClient DYNAMO_DB;

	private final AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Autowired
	private DynamoDbLockRepository dynamoDbLockRepository;

	@Autowired
	private DynamoDbLockRegistry dynamoDbLockRegistry;

	@BeforeAll
	static void setup() {
		DYNAMO_DB = LocalstackContainerTest.dynamoDbAsyncClient();
		try {
			DYNAMO_DB.deleteTable(request -> request.tableName(DynamoDbLockRepository.DEFAULT_TABLE_NAME))
					.thenCompose(
							result -> DYNAMO_DB.waiter().waitUntilTableNotExists(
									request -> request.tableName(DynamoDbLockRepository.DEFAULT_TABLE_NAME),
									waiter -> waiter.maxAttempts(25)
											.backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1)))))
					.get();
		}
		catch (Exception e) {
			// Ignore
		}
	}

	@BeforeEach
	void clear() throws InterruptedException {
		CountDownLatch createTableLatch = (CountDownLatch) ReflectionTestUtils.getField(this.dynamoDbLockRepository,
				"createTableLatch");

		createTableLatch.await();
		this.dynamoDbLockRepository.close();
	}

	@Test
	void lock() {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.dynamoDbLockRegistry.obtain("foo");
			lock.lock();
			try {
				assertThat((Set<?>) ReflectionTestUtils.getField(this.dynamoDbLockRepository, "heldLocks")).hasSize(1);
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	void lockInterruptibly() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock = this.dynamoDbLockRegistry.obtain("foo");
			lock.lockInterruptibly();
			try {
				assertThat((Set<?>) ReflectionTestUtils.getField(this.dynamoDbLockRepository, "heldLocks")).hasSize(1);
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Test
	void reentrantLock() {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.dynamoDbLockRegistry.obtain("foo");
			lock1.lock();
			try {
				Lock lock2 = this.dynamoDbLockRegistry.obtain("foo");
				assertThat(lock1).isSameAs(lock2);
				lock2.lock();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void reentrantLockInterruptibly() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.dynamoDbLockRegistry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = this.dynamoDbLockRegistry.obtain("foo");
				assertThat(lock1).isSameAs(lock2);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void twoLocks() throws Exception {
		for (int i = 0; i < 10; i++) {
			Lock lock1 = this.dynamoDbLockRegistry.obtain("foo");
			lock1.lockInterruptibly();
			try {
				Lock lock2 = this.dynamoDbLockRegistry.obtain("bar");
				assertThat(lock1).isNotSameAs(lock2);
				lock2.lockInterruptibly();
				lock2.unlock();
			}
			finally {
				lock1.unlock();
			}
		}
	}

	@Test
	void twoThreadsSecondFailsToGetLock() throws Exception {
		final Lock lock1 = this.dynamoDbLockRegistry.obtain("foo");
		lock1.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = this.taskExecutor.submit(() -> {
			DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(DYNAMO_DB);
			dynamoDbLockRepository.afterPropertiesSet();
			DynamoDbLockRegistry registry2 = new DynamoDbLockRegistry(dynamoDbLockRepository);
			registry2.setIdleBetweenTries(Duration.ofMillis(10));
			registry2.setTimeToLive(Duration.ofSeconds(10));
			Lock lock2 = registry2.obtain("foo");
			locked.set(lock2.tryLock());
			latch.countDown();
			try {
				lock2.unlock();
			}
			catch (Exception e) {
				return e;
			}
			finally {
				dynamoDbLockRepository.close();
			}
			return null;
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();
		lock1.unlock();
		Object ise = result.get(10, TimeUnit.SECONDS);
		assertThat(ise).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) ise).getMessage()).contains("The current thread doesn't own mutex at 'foo'");
	}

	@Test
	void twoThreads() throws Exception {
		final Lock lock1 = this.dynamoDbLockRegistry.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		this.taskExecutor.submit(() -> {
			DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(DYNAMO_DB);
			dynamoDbLockRepository.afterPropertiesSet();
			DynamoDbLockRegistry registry2 = new DynamoDbLockRegistry(dynamoDbLockRepository);
			registry2.setIdleBetweenTries(Duration.ofMillis(10));
			registry2.setTimeToLive(Duration.ofSeconds(10));
			Lock lock2 = registry2.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				latch2.await(10, TimeUnit.SECONDS);
				locked.set(true);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				lock2.unlock();
				latch3.countDown();
				dynamoDbLockRepository.close();
			}
			return null;
		});

		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();

		lock1.unlock();
		latch2.countDown();

		assertThat(latch3.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isTrue();
	}

	@Test
	void twoThreadsDifferentRegistries() throws Exception {
		DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(DYNAMO_DB);
		dynamoDbLockRepository.afterPropertiesSet();
		final DynamoDbLockRegistry registry1 = new DynamoDbLockRegistry(dynamoDbLockRepository);
		registry1.setIdleBetweenTries(Duration.ofMillis(10));
		registry1.setTimeToLive(Duration.ofSeconds(10));

		final DynamoDbLockRegistry registry2 = new DynamoDbLockRegistry(dynamoDbLockRepository);
		registry2.setIdleBetweenTries(Duration.ofMillis(10));
		registry2.setTimeToLive(Duration.ofSeconds(10));

		final Lock lock1 = registry1.obtain("foo");
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		this.taskExecutor.execute(() -> {
			Lock lock2 = registry2.obtain("foo");
			try {
				latch1.countDown();
				lock2.lockInterruptibly();
				latch2.await(10, TimeUnit.SECONDS);
				locked.set(true);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				lock2.unlock();
				latch3.countDown();
			}
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();

		lock1.unlock();
		latch2.countDown();

		assertThat(latch3.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isTrue();

		dynamoDbLockRepository.close();
	}

	@Test
	void twoThreadsWrongOneUnlocks() throws Exception {
		final Lock lock = this.dynamoDbLockRegistry.obtain("foo");
		lock.lockInterruptibly();
		final AtomicBoolean locked = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		Future<Object> result = this.taskExecutor.submit(() -> {
			try {
				lock.unlock();
			}
			catch (Exception e) {
				latch.countDown();
				return e;
			}
			return null;
		});

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(locked.get()).isFalse();

		lock.unlock();
		Object imse = result.get(10, TimeUnit.SECONDS);
		assertThat(imse).isInstanceOf(IllegalMonitorStateException.class);
		assertThat(((Exception) imse).getMessage()).contains("The current thread doesn't own mutex at 'foo'");
	}

	@Test
	void abandonedLock() throws Exception {
		DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(DYNAMO_DB);
		dynamoDbLockRepository.afterPropertiesSet();
		this.dynamoDbLockRepository.acquire("foo", Duration.ofSeconds(10));

		Lock lock = this.dynamoDbLockRegistry.obtain("foo");
		int n = 0;
		while (!lock.tryLock() && n++ < 100) {
			Thread.sleep(100);
		}

		assertThat(n).isLessThan(100);
		lock.unlock();
		dynamoDbLockRepository.close();
	}

	@Test
	void lockRenew() {
		final Lock lock = this.dynamoDbLockRegistry.obtain("foo");

		assertThat(lock.tryLock()).isTrue();
		try {
			this.dynamoDbLockRegistry.setTimeToLive(Duration.ofSeconds(60));
			assertThatNoException().isThrownBy(() -> this.dynamoDbLockRegistry.renewLock("foo"));
			String ttl = DYNAMO_DB
					.getItem(request -> request.tableName(DynamoDbLockRepository.DEFAULT_TABLE_NAME)
							.key(Map.of(DynamoDbLockRepository.KEY_ATTR, AttributeValue.fromS("foo"))))
					.join().item().get(DynamoDbLockRepository.TTL_ATTR).n();
			assertThat(Long.parseLong(ttl)).isCloseTo(LocalDateTime.now().plusSeconds(60).toEpochSecond(ZoneOffset.UTC),
					Percentage.withPercentage(10));
		}
		finally {
			lock.unlock();
			this.dynamoDbLockRegistry.setTimeToLive(Duration.ofSeconds(2));
		}
	}

	@Configuration(proxyBeanMethods = false)
	public static class ContextConfiguration {

		@Bean
		public DynamoDbLockRepository dynamoDbLockRepository() {
			return new DynamoDbLockRepository(DYNAMO_DB);
		}

		@Bean
		public DynamoDbLockRegistry dynamoDbLockRegistry(DynamoDbLockRepository dynamoDbLockRepository) {
			DynamoDbLockRegistry dynamoDbLockRegistry = new DynamoDbLockRegistry(dynamoDbLockRepository);
			dynamoDbLockRegistry.setIdleBetweenTries(Duration.ofMillis(100));
			dynamoDbLockRegistry.setTimeToLive(Duration.ofSeconds(2));
			return dynamoDbLockRegistry;
		}

	}

}
