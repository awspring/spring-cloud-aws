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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

/**
 * The {@link ConcurrentMetadataStore} for the {@link DynamoDbAsyncClient}.
 *
 * @author Artem Bilan
 * @author Asiel Caballero
 *
 * @since 4.0
 */
public class DynamoDbMetadataStore implements ConcurrentMetadataStore, InitializingBean {

	private static final Log logger = LogFactory.getLog(DynamoDbMetadataStore.class);

	/**
	 * The {@value DEFAULT_TABLE_NAME} default name for the metadata table in the DynamoDB.
	 */
	public static final String DEFAULT_TABLE_NAME = "SpringIntegrationMetadataStore";

	/**
	 * The {@value KEY} as a default name for a partition key in the table.
	 */
	public static final String KEY = "metadataKey";

	/**
	 * The {@value VALUE} as a default name for value attribute.
	 */
	public static final String VALUE = "metadataValue";

	/**
	 * The {@value TTL} as a default name for time-to-live attribute.
	 */
	public static final String TTL = "expireAt";

	private static final String KEY_NOT_EXISTS_EXPRESSION = String.format("attribute_not_exists(%s)", KEY);

	private final DynamoDbAsyncClient dynamoDB;

	private final String tableName;

	private final CountDownLatch createTableLatch = new CountDownLatch(1);

	private int createTableRetries = 25;

	private int createTableDelay = 1;

	private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

	private long readCapacity = 1L;

	private long writeCapacity = 1L;

	private Integer timeToLive;

	private volatile boolean initialized;

	public DynamoDbMetadataStore(DynamoDbAsyncClient dynamoDB) {
		this(dynamoDB, DEFAULT_TABLE_NAME);
	}

	public DynamoDbMetadataStore(DynamoDbAsyncClient dynamoDB, String tableName) {
		Assert.notNull(dynamoDB, "'dynamoDB' must not be null.");
		Assert.hasText(tableName, "'tableName' must not be empty.");
		this.dynamoDB = dynamoDB;
		this.tableName = tableName;

	}

	public void setCreateTableRetries(int createTableRetries) {
		this.createTableRetries = createTableRetries;
	}

	public void setCreateTableDelay(int createTableDelay) {
		this.createTableDelay = createTableDelay;
	}

	public void setBillingMode(BillingMode billingMode) {
		Assert.notNull(billingMode, "'billingMode' must not be null");
		this.billingMode = billingMode;
	}

	public void setReadCapacity(long readCapacity) {
		this.readCapacity = readCapacity;
	}

	public void setWriteCapacity(long writeCapacity) {
		this.writeCapacity = writeCapacity;
	}

	/**
	 * Configure a period in seconds for item expiration. If it is configured to non-positive value ({@code <= 0}), the
	 * TTL is disabled on the table.
	 * @param timeToLive period in seconds for item expiration.
	 * @since 2.0
	 * @see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/TTL.html">DynamoDB TTL</a>
	 */
	public void setTimeToLive(int timeToLive) {
		this.timeToLive = timeToLive;
	}

	@Override
	public void afterPropertiesSet() {
		this.dynamoDB.describeTable(request -> request.tableName(this.tableName)).thenRun(() -> {
		}).exceptionallyCompose((ex) -> {
			Throwable cause = ex.getCause();
			if (cause instanceof ResourceNotFoundException) {
				if (logger.isInfoEnabled()) {
					logger.info("No table '" + this.tableName + "'. Creating one...");
				}
				return createTable();
			}
			else {
				return rethrowAsRuntimeException(cause);
			}
		}).thenCompose(result -> updateTimeToLiveIfAny()).exceptionally((ex) -> {
			logger.error("Cannot create DynamoDb table: " + this.tableName, ex.getCause());
			return null;
		}).thenRun(this.createTableLatch::countDown);

		this.initialized = true;
	}

	private CompletableFuture<Void> createTable() {
		CreateTableRequest.Builder createTableRequest = CreateTableRequest.builder().tableName(this.tableName)
				.keySchema(KeySchemaElement.builder().attributeName(KEY).keyType(KeyType.HASH).build())
				.attributeDefinitions(
						AttributeDefinition.builder().attributeName(KEY).attributeType(ScalarAttributeType.S).build())
				.billingMode(this.billingMode);

		if (BillingMode.PROVISIONED.equals(this.billingMode)) {
			createTableRequest.provisionedThroughput(ProvisionedThroughput.builder()
					.readCapacityUnits(this.readCapacity).writeCapacityUnits(this.writeCapacity).build());
		}

		return this.dynamoDB.createTable(createTableRequest.build())
				.thenCompose(result -> this.dynamoDB.waiter().waitUntilTableExists(
						request -> request.tableName(this.tableName),
						waiter -> waiter.maxAttempts(this.createTableRetries).backoffStrategy(
								FixedDelayBackoffStrategy.create(Duration.ofSeconds(this.createTableDelay)))))
				.thenRun(() -> {
				});
	}

	private CompletableFuture<?> updateTimeToLiveIfAny() {
		if (this.timeToLive != null) {
			UpdateTimeToLiveRequest.Builder updateTimeToLiveRequest = UpdateTimeToLiveRequest.builder()
					.tableName(this.tableName)
					.timeToLiveSpecification(ttl -> ttl.attributeName(TTL).enabled(this.timeToLive > 0));

			return this.dynamoDB.updateTimeToLive(updateTimeToLiveRequest.build()).exceptionally((ex) -> {
				if (logger.isWarnEnabled()) {
					logger.warn("The error during 'updateTimeToLive' request", ex);
				}
				return null;
			});
		}

		return CompletableFuture.completedFuture(null);
	}

	private void awaitForActive() {
		Assert.state(this.initialized,
				() -> "The component has not been initialized: " + this + ".\n Is it declared as a bean?");
		try {
			this.createTableLatch.await(this.createTableRetries * this.createTableDelay, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("The DynamoDb table " + this.tableName + " has not been created during "
					+ this.createTableRetries * this.createTableDelay + " seconds");
		}
	}

	@Override
	public void put(String key, String value) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(value, "'value' must not be empty.");

		awaitForActive();

		Map<String, AttributeValue> attributes = new HashMap<>();
		attributes.put(KEY, AttributeValue.fromS(key));
		attributes.put(VALUE, AttributeValue.fromS(value));

		if (this.timeToLive != null && this.timeToLive > 0) {
			attributes.put(TTL, AttributeValue.fromN("" + Instant.now().plusMillis(this.timeToLive).getEpochSecond()));
		}

		PutItemRequest.Builder putItemRequest = PutItemRequest.builder().tableName(this.tableName).item(attributes);

		this.dynamoDB.putItem(putItemRequest.build()).join();
	}

	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty.");

		awaitForActive();

		try {
			return this.dynamoDB
					.getItem(request -> request.tableName(this.tableName).key(Map.of(KEY, AttributeValue.fromS(key))))
					.thenApply(GetItemResponse::item).thenApply(DynamoDbMetadataStore::getValueIfAny).join();
		}
		catch (CompletionException ex) {
			return rethrowAsRuntimeException(ex.getCause());
		}
	}

	@Override
	public String putIfAbsent(String key, String value) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(value, "'value' must not be empty.");

		awaitForActive();

		Map<String, AttributeValue> attributes = new HashMap<>();
		attributes.put(":value", AttributeValue.fromS(value));

		String updateExpression = "SET " + VALUE + " = :value";

		if (this.timeToLive != null && this.timeToLive > 0) {
			updateExpression += ", " + TTL + " = :ttl";
			attributes.put(":ttl",
					AttributeValue.fromN("" + Instant.now().plusMillis(this.timeToLive).getEpochSecond()));
		}

		UpdateItemRequest.Builder updateItemRequest = UpdateItemRequest.builder().tableName(this.tableName)
				.key(Map.of(KEY, AttributeValue.fromS(key))).conditionExpression(KEY_NOT_EXISTS_EXPRESSION)
				.updateExpression(updateExpression).expressionAttributeValues(attributes);

		try {
			this.dynamoDB.updateItem(updateItemRequest.build()).join();
			return null;
		}
		catch (CompletionException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof ConditionalCheckFailedException) {
				return get(key);
			}
			else {
				return rethrowAsRuntimeException(cause);
			}
		}
	}

	@Override
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.hasText(key, "'key' must not be empty.");
		Assert.hasText(oldValue, "'value' must not be empty.");
		Assert.hasText(newValue, "'newValue' must not be empty.");

		awaitForActive();

		Map<String, AttributeValue> attributes = new HashMap<>();
		attributes.put(":newValue", AttributeValue.fromS(newValue));
		attributes.put(":oldValue", AttributeValue.fromS(oldValue));

		String updateExpression = "SET " + VALUE + " = :newValue";

		if (this.timeToLive != null && this.timeToLive > 0) {
			updateExpression += ", " + TTL + " = :ttl";
			attributes.put(":ttl",
					AttributeValue.fromN("" + Instant.now().plusMillis(this.timeToLive).getEpochSecond()));
		}

		UpdateItemRequest.Builder updateItemRequest = UpdateItemRequest.builder().tableName(this.tableName)
				.key(Map.of(KEY, AttributeValue.fromS(key))).conditionExpression(VALUE + " = :oldValue")
				.updateExpression(updateExpression).expressionAttributeValues(attributes)
				.returnValues(ReturnValue.UPDATED_NEW);

		try {
			return this.dynamoDB.updateItem(updateItemRequest.build()).join().hasAttributes();
		}
		catch (CompletionException ex) {
			if (ex.getCause() instanceof ConditionalCheckFailedException) {
				return false;
			}
			else {
				return rethrowAsRuntimeException(ex.getCause());
			}
		}
	}

	@Override
	public String remove(String key) {
		Assert.hasText(key, "'key' must not be empty.");

		awaitForActive();

		try {
			return this.dynamoDB
					.deleteItem(request -> request.tableName(this.tableName).key(Map.of(KEY, AttributeValue.fromS(key)))
							.returnValues(ReturnValue.ALL_OLD))
					.thenApply(DeleteItemResponse::attributes).thenApply(DynamoDbMetadataStore::getValueIfAny).join();
		}
		catch (CompletionException ex) {
			return rethrowAsRuntimeException(ex.getCause());
		}
	}

	private static String getValueIfAny(Map<String, AttributeValue> item) {
		if (item.containsKey(VALUE)) {
			return item.get(VALUE).s();
		}
		else {
			return null;
		}
	}

	private static <T> T rethrowAsRuntimeException(Throwable cause) {
		if (cause instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		else {
			throw new IllegalStateException(cause);
		}
	}

	@Override
	public String toString() {
		return "DynamoDbMetadataStore{" + "table=" + this.tableName + ", createTableRetries=" + this.createTableRetries
				+ ", createTableDelay=" + this.createTableDelay + ", billingMode=" + this.billingMode
				+ ", readCapacity=" + this.readCapacity + ", writeCapacity=" + this.writeCapacity + ", timeToLive="
				+ this.timeToLive + '}';
	}
}
