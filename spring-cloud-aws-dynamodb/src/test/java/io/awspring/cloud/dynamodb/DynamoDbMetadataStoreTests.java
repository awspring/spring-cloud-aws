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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
class DynamoDbMetadataStoreTests implements LocalstackContainerTest {

	private static final String TEST_TABLE = "testMetadataStore";

	private static DynamoDbAsyncClient DYNAMO_DB;

	private static DynamoDbMetadataStore store;

	private final String file1 = "/remotepath/filesTodownload/file-1.txt";

	private final String file1Id = "12345";

	@BeforeAll
	static void setup() {
		DYNAMO_DB = LocalstackContainerTest.dynamoDbAsyncClient();
		try {
			DYNAMO_DB.deleteTable(request -> request.tableName(TEST_TABLE))
					.thenCompose(
							result -> DYNAMO_DB.waiter().waitUntilTableNotExists(
									request -> request.tableName(DynamoDbLockRepository.DEFAULT_TABLE_NAME),
									waiter -> waiter.maxAttempts(25)
											.backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1)))))
					.join();
		}
		catch (Exception e) {
			// Ignore if the table does not exist
		}

		store = new DynamoDbMetadataStore(DYNAMO_DB, TEST_TABLE);
		store.setTimeToLive(10);
		store.afterPropertiesSet();
	}

	@BeforeEach
	void clear() throws InterruptedException {
		CountDownLatch createTableLatch = (CountDownLatch) ReflectionTestUtils.getField(store, "createTableLatch");

		createTableLatch.await();

		DYNAMO_DB.deleteItem(request -> request.tableName(TEST_TABLE)
				.key(Map.of(DynamoDbMetadataStore.KEY, AttributeValue.fromS((this.file1))))).join();
	}

	@Test
	void getFromStore() {
		String fileID = store.get(this.file1);
		assertThat(fileID).isNull();

		store.put(this.file1, this.file1Id);

		fileID = store.get(this.file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo(this.file1Id);
	}

	@Test
	void putIfAbsent() {
		String fileID = store.get(this.file1);
		assertThat(fileID).describedAs("Get First time, Value must not exist").isNull();

		fileID = store.putIfAbsent(this.file1, this.file1Id);
		assertThat(fileID).describedAs("Insert First time, Value must return null").isNull();

		fileID = store.putIfAbsent(this.file1, "56789");
		assertThat(fileID).describedAs("Key Already Exists - Insertion Failed, ol value must be returned").isNotNull();
		assertThat(fileID).describedAs("The Old Value must be equal to returned").isEqualTo(this.file1Id);

		assertThat(store.get(this.file1)).describedAs("The Old Value must return").isEqualTo(this.file1Id);
	}

	@Test
	void remove() {
		String fileID = store.remove(this.file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(this.file1, this.file1Id);
		assertThat(fileID).isNull();

		fileID = store.remove(this.file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo(this.file1Id);

		fileID = store.get(this.file1);
		assertThat(fileID).isNull();
	}

	@Test
	void replace() {
		boolean removedValue = store.replace(this.file1, this.file1Id, "4567");
		assertThat(removedValue).isFalse();

		String fileID = store.get(this.file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(this.file1, this.file1Id);
		assertThat(fileID).isNull();

		removedValue = store.replace(this.file1, this.file1Id, "4567");
		assertThat(removedValue).isTrue();

		fileID = store.get(this.file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo("4567");
	}

}
