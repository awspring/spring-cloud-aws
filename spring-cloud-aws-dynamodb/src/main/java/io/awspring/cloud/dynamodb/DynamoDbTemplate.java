/*
 * Copyright 2013-2023 the original author or authors.
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

import java.util.Collections;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/**
 * Default implementation of {@link DynamoDbOperations}.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class DynamoDbTemplate implements DynamoDbOperations {
	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver;
	private final DynamoDbTableNameResolver tableNameResolver;

	public DynamoDbTemplate(@Nullable String tablePrefix, DynamoDbEnhancedClient dynamoDbEnhancedClient) {
		this(dynamoDbEnhancedClient, new DefaultDynamoDbTableSchemaResolver(Collections.emptyList()),
				new DefaultDynamoDbTableNameResolver(tablePrefix));
	}

	public DynamoDbTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
		this(dynamoDbEnhancedClient, new DefaultDynamoDbTableSchemaResolver(Collections.emptyList()),
				new DefaultDynamoDbTableNameResolver());
	}

	public DynamoDbTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient,
			DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver, DynamoDbTableNameResolver tableNameResolver) {
		this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
		this.dynamoDbTableSchemaResolver = dynamoDbTableSchemaResolver;
		this.tableNameResolver = tableNameResolver;
	}

	public <T> T save(T entity) {
		Assert.notNull(entity, "entity is required");
		prepareTable(entity).putItem(entity);
		return entity;
	}

	public <T> T update(T entity) {
		Assert.notNull(entity, "entity is required");
		return prepareTable(entity).updateItem(entity);
	}

	public <T> T delete(Key key, Class<T> clazz) {
		Assert.notNull(key, "key is required");
		Assert.notNull(clazz, "clazz is required");
		 return prepareTable(clazz).deleteItem(key);
	}

	public <T> T delete(T entity) {
		Assert.notNull(entity, "entity is required");
		return prepareTable(entity).deleteItem(entity);
	}

	@Nullable
	public <T> T load(Key key, Class<T> clazz) {
		Assert.notNull(key, "key is required");
		Assert.notNull(clazz, "clazz is required");
		return prepareTable(clazz).getItem(key);
	}

	public <T> PageIterable<T> scan(ScanEnhancedRequest scanEnhancedRequest, Class<T> clazz) {
		Assert.notNull(scanEnhancedRequest, "scanEnhancedRequest is required");
		Assert.notNull(clazz, "clazz is required");
		return prepareTable(clazz).scan(scanEnhancedRequest);
	}

	public <T> PageIterable<T> scan(ScanEnhancedRequest scanEnhancedRequest, Class<T> clazz, String indexName) {
		return PageIterable.create(prepareTable(clazz).index(indexName).scan(scanEnhancedRequest));
	}

	public <T> PageIterable<T> scanAll(Class<T> clazz, String indexName) {
		return PageIterable.create(prepareTable(clazz).index(indexName).scan());
	}

	public <T> PageIterable<T> query(QueryEnhancedRequest queryEnhancedRequest, Class<T> clazz, String indexName) {
		return PageIterable.create(prepareTable(clazz).index(indexName).query(queryEnhancedRequest));
	}

	public <T> PageIterable<T> scanAll(Class<T> clazz) {
		Assert.notNull(clazz, "clazz is required");
		return prepareTable(clazz).scan();
	}

	public <T> PageIterable<T> query(QueryEnhancedRequest queryEnhancedRequest, Class<T> clazz) {
		Assert.notNull(queryEnhancedRequest, "queryEnhancedRequest is required");
		Assert.notNull(clazz, "clazz is required");
		return prepareTable(clazz).query(queryEnhancedRequest);
	}

	private <T> DynamoDbTable<T> prepareTable(T entity) {
		Assert.notNull(entity, "entity is required");
		String tableName = tableNameResolver.resolve(entity.getClass());
		return (DynamoDbTable<T>) dynamoDbEnhancedClient.table(tableName,
				dynamoDbTableSchemaResolver.resolve(entity.getClass()));
	}

	private <T> DynamoDbTable<T> prepareTable(Class<T> clazz) {
		Assert.notNull(clazz, "clazz is required");
		String tableName = tableNameResolver.resolve(clazz);
		return dynamoDbEnhancedClient.table(tableName, dynamoDbTableSchemaResolver.resolve(clazz));
	}

}
