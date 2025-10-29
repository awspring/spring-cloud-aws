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

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.Select;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * Encapsulation of the DynamoDB shunting that is needed for locks.
 * <p>
 * The DynamoDb table must have these attributes:
 * <ul>
 * <li>{@link DynamoDbLockRepository#KEY_ATTR} {@link ScalarAttributeType#S} - partition key {@link KeyType#HASH}
 * <li>{@link DynamoDbLockRepository#OWNER_ATTR} {@link ScalarAttributeType#S}
 * <li>{@link DynamoDbLockRepository#CREATED_ATTR} {@link ScalarAttributeType#N}
 * <li>{@link DynamoDbLockRepository#TTL_ATTR} {@link ScalarAttributeType#N}
 * </ul>
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class DynamoDbLockRepository implements InitializingBean, DisposableBean, Closeable {

	/**
	 * The {@value DEFAULT_TABLE_NAME} default name for the locks table in the DynamoDB.
	 */
	public static final String DEFAULT_TABLE_NAME = "SpringIntegrationLockRegistry";

	/**
	 * The {@value KEY_ATTR} name for the partition key in the table.
	 */
	public static final String KEY_ATTR = "lockKey";

	/**
	 * The {@value OWNER_ATTR} name for the owner of lock in the table.
	 */
	public static final String OWNER_ATTR = "lockOwner";

	/**
	 * The {@value CREATED_ATTR} date for lock item.
	 */
	public static final String CREATED_ATTR = "createdAt";

	/**
	 * The {@value TTL_ATTR} for how long the lock is valid.
	 */
	public static final String TTL_ATTR = "expireAt";

	private static final String LOCK_EXISTS_EXPRESSION = String.format("attribute_exists(%s) AND %s = :owner", KEY_ATTR,
			OWNER_ATTR);

	private static final String LOCK_NOT_EXISTS_EXPRESSION = String
			.format("attribute_not_exists(%s) OR %s = :owner OR %s < :ttl", KEY_ATTR, OWNER_ATTR, TTL_ATTR);

	private static final Log LOGGER = LogFactory.getLog(DynamoDbLockRepository.class);

	private final CountDownLatch createTableLatch = new CountDownLatch(1);

	private final Set<String> heldLocks = Collections.synchronizedSet(new HashSet<>());

	private final DynamoDbAsyncClient dynamoDB;

	private final String tableName;

	private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

	private long readCapacity = 1L;

	private long writeCapacity = 1L;

	private String owner = UUID.randomUUID().toString();

	private Map<String, AttributeValue> ownerAttribute;

	private volatile boolean initialized;

	public DynamoDbLockRepository(DynamoDbAsyncClient dynamoDB) {
		this(dynamoDB, DEFAULT_TABLE_NAME);
	}

	public DynamoDbLockRepository(DynamoDbAsyncClient dynamoDB, String tableName) {
		this.dynamoDB = dynamoDB;
		this.tableName = tableName;
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
	 * Specify a custom client id (owner) for locks in DB. Must be unique per cluster to avoid interlocking between
	 * different instances.
	 * @param owner the client id to be associated with locks handled by the repository.
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getTableName() {
		return this.tableName;
	}

	public String getOwner() {
		return this.owner;
	}

	@Override
	public void afterPropertiesSet() {

		this.dynamoDB.describeTable(request -> request.tableName(this.tableName)).thenRun(() -> {
		}).exceptionallyCompose((ex) -> {
			Throwable cause = ex.getCause();
			if (cause instanceof ResourceNotFoundException) {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("No table '" + getTableName() + "'. Creating one...");
				}
				return createTable();
			}
			else {
				return rethrowAsRuntimeException(cause);
			}
		}).exceptionally((ex) -> {
			LOGGER.error("Cannot create DynamoDb table: " + this.tableName, ex.getCause());
			return null;
		}).thenRun(this.createTableLatch::countDown);

		this.ownerAttribute = Map.of(":owner", AttributeValue.fromS(this.owner));
		this.initialized = true;
	}

	/*
	 * Creates a DynamoDB table with the right schema for it to be used by this locking library. The table should be set
	 * up in advance because it takes a few minutes for DynamoDB to provision a new instance. If the table already
	 * exists, no exception.
	 */
	private CompletableFuture<Void> createTable() {
		CreateTableRequest.Builder createTableRequest = CreateTableRequest.builder().tableName(this.tableName)
				.keySchema(KeySchemaElement.builder().attributeName(KEY_ATTR).keyType(KeyType.HASH).build())
				.attributeDefinitions(AttributeDefinition.builder().attributeName(KEY_ATTR)
						.attributeType(ScalarAttributeType.S).build())
				.billingMode(this.billingMode);

		if (BillingMode.PROVISIONED.equals(this.billingMode)) {
			createTableRequest.provisionedThroughput(ProvisionedThroughput.builder()
					.readCapacityUnits(this.readCapacity).writeCapacityUnits(this.writeCapacity).build());
		}

		return this.dynamoDB.createTable(createTableRequest.build())
				.thenCompose(result -> this.dynamoDB.waiter().waitUntilTableExists(
						request -> request.tableName(this.tableName),
						waiter -> waiter.maxAttempts(60)
								.backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1)))))
				.thenCompose((response) -> updateTimeToLive()).thenRun(() -> {
				});
	}

	private CompletableFuture<?> updateTimeToLive() {
		return this.dynamoDB.updateTimeToLive(ttlRequest -> ttlRequest.tableName(this.tableName)
				.timeToLiveSpecification(ttlSpec -> ttlSpec.enabled(true).attributeName(TTL_ATTR)));
	}

	private void awaitForActive() {
		Assert.state(this.initialized,
				() -> "The component has not been initialized: " + this + ".\n Is it declared as a bean?");

		try {
			if (!this.createTableLatch.await(60, TimeUnit.SECONDS)) {
				throw new IllegalStateException(
						"The DynamoDb table " + getTableName() + " has not been created during " + 60 + " seconds");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(
					"The DynamoDb table " + getTableName() + " has not been created and waiting thread is interrupted");
		}
	}

	/**
	 * Check if a lock is held by this repository.
	 * @param lock the lock to check.
	 * @return acquired or not.
	 */
	public boolean isAcquired(String lock) {
		awaitForActive();
		if (this.heldLocks.contains(lock)) {
			Map<String, AttributeValue> values = ownerWithTtlValues(currentEpochSeconds());
			values.put(":lock", AttributeValue.fromS(lock));

			QueryRequest.Builder queryRequest = QueryRequest.builder().tableName(this.tableName).select(Select.COUNT)
					.limit(1).keyConditionExpression(KEY_ATTR + " = :lock")
					.filterExpression(OWNER_ATTR + " = :owner AND " + TTL_ATTR + " >= :ttl")
					.expressionAttributeValues(values);

			try {
				return this.dynamoDB.query(queryRequest.build()).get().count() > 0;
			}
			catch (CompletionException | ExecutionException ex) {
				rethrowAsRuntimeException(ex.getCause());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return rethrowAsRuntimeException(ex);
			}
		}
		return false;
	}

	/**
	 * Remove a lock from this repository.
	 * @param lock the lock to remove.
	 */
	public void delete(String lock) {
		awaitForActive();
		if (this.heldLocks.remove(lock)) {
			deleteFromDb(lock);
		}
	}

	private void deleteFromDb(String lock) {
		doDelete(DeleteItemRequest.builder().key(Map.of(KEY_ATTR, AttributeValue.fromS(lock)))
				.conditionExpression(OWNER_ATTR + " = :owner").expressionAttributeValues(this.ownerAttribute));
	}

	private void doDelete(DeleteItemRequest.Builder deleteItemRequest) {
		try {
			this.dynamoDB.deleteItem(deleteItemRequest.tableName(this.tableName).build()).get();
		}
		catch (CompletionException | ExecutionException ex) {
			Throwable cause = ex.getCause();
			// Ignore - assuming no record in DB anymore.
			if (!(cause instanceof ConditionalCheckFailedException)) {
				rethrowAsRuntimeException(cause);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			rethrowAsRuntimeException(ex);
		}
	}

	/**
	 * Remove all the expired locks.
	 */
	public void deleteExpired() {
		awaitForActive();
		synchronized (this.heldLocks) {
			this.heldLocks.forEach(
					(lock) -> doDelete(DeleteItemRequest.builder().key(Map.of(KEY_ATTR, AttributeValue.fromS(lock)))
							.conditionExpression(OWNER_ATTR + " = :owner AND " + TTL_ATTR + " < :ttl")
							.expressionAttributeValues(ownerWithTtlValues(currentEpochSeconds()))));
			this.heldLocks.clear();
		}
	}

	private Map<String, AttributeValue> ownerWithTtlValues(long epochSeconds) {
		Map<String, AttributeValue> valueMap = new HashMap<>();
		valueMap.put(":ttl", AttributeValue.fromN("" + epochSeconds));
		valueMap.putAll(this.ownerAttribute);
		return valueMap;
	}

	/**
	 * Acquire a lock for a key.
	 *
	 * @param lock the key for lock to acquire.
	 * @param ttl the lease duration for the lock record.
	 * @return acquired or not.
	 */
	public boolean acquire(String lock, Duration ttl) throws InterruptedException {
		awaitForActive();
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		long currentTime = currentEpochSeconds();

		Map<String, AttributeValue> item = new HashMap<>();
		item.put(KEY_ATTR, AttributeValue.fromS(lock));
		item.put(OWNER_ATTR, AttributeValue.fromS(this.owner));
		item.put(CREATED_ATTR, AttributeValue.fromN("" + currentTime));
		item.put(TTL_ATTR, AttributeValue.fromN("" + ttlEpochSeconds(ttl)));
		PutItemRequest.Builder putItemRequest = PutItemRequest.builder().tableName(this.tableName).item(item)
				.conditionExpression(LOCK_NOT_EXISTS_EXPRESSION)
				.expressionAttributeValues(ownerWithTtlValues(currentTime));
		try {
			this.dynamoDB.putItem(putItemRequest.build()).thenRun(() -> this.heldLocks.add(lock)).get();
			return true;
		}
		catch (CompletionException | ExecutionException ex) {
			Throwable cause = ex.getCause();
			if (!(cause instanceof ConditionalCheckFailedException)) {
				rethrowAsRuntimeException(cause);
			}
			return false;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw ex;
		}
	}

	/**
	 * Renew the lease for a lock.
	 *
	 * @param lock the lock to renew.
	 * @param ttl a new lease duration
	 * @return renewed or not.
	 */
	public boolean renew(String lock, Duration ttl) {
		awaitForActive();
		if (this.heldLocks.contains(lock)) {
			UpdateItemRequest.Builder updateItemRequest = UpdateItemRequest.builder().tableName(this.tableName)
					.key(Map.of(KEY_ATTR, AttributeValue.fromS(lock))).updateExpression("SET " + TTL_ATTR + " = :ttl")
					.conditionExpression(LOCK_EXISTS_EXPRESSION)
					.expressionAttributeValues(ownerWithTtlValues(ttlEpochSeconds(ttl)));
			try {
				this.dynamoDB.updateItem(updateItemRequest.build()).get();
				return true;
			}
			catch (CompletionException | ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (!(cause instanceof ConditionalCheckFailedException)) {
					rethrowAsRuntimeException(cause);
				}
				return false;
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return rethrowAsRuntimeException(ex.getCause());
			}
		}
		return false;
	}

	@Override
	public void destroy() {
		close();
	}

	@Override
	public void close() {
		synchronized (this.heldLocks) {
			this.heldLocks.forEach(this::deleteFromDb);
			this.heldLocks.clear();
		}
	}

	private long ttlEpochSeconds(Duration ttl) {
		return Instant.now().plus(ttl).getEpochSecond();
	}

	private static long currentEpochSeconds() {
		return Instant.now().getEpochSecond();
	}

	private static <T> T rethrowAsRuntimeException(Throwable cause) {
		if (cause instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}
		else {
			throw new IllegalStateException(cause);
		}
	}

}
