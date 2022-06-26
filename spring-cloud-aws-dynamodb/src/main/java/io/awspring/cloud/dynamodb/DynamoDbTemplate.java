/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.lang.Nullable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/**
 * Default implementation of DynamoDbOperations.
 *
 * @author Matej Nedic
 */
public class DynamoDbTemplate implements DynamoDbOperations {
	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver;
	private final DynamoDbTableNameResolver dynamoDbTableNameResolver;

	public DynamoDbTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
		this(dynamoDbEnhancedClient, new DefaultDynamoDbTableSchemaResolver(), new DefaultDynamoDbTableNameResolver());
	}

	public DynamoDbTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient,
			DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver,
			DynamoDbTableNameResolver dynamoDbTableNameResolver) {
		this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
		this.dynamoDbTableSchemaResolver = dynamoDbTableSchemaResolver;
		this.dynamoDbTableNameResolver = dynamoDbTableNameResolver;
	}

	public <T> T save(T entity) {
		prepareTable(entity).putItem(entity);
		return entity;
	}

	public <T> T update(T entity) {
		return prepareTable(entity).updateItem(entity);
	}

	public void delete(Key key, Class<?> clazz) {
		prepareTable(clazz).deleteItem(key);
	}

	public <T> void delete(T entity) {
		prepareTable(entity).deleteItem(entity);
	}

	@Nullable
	public <T> T load(Key key, Class<T> clazz) {
		return prepareTable(clazz).getItem(key);
	}

	public <T> PageIterable<T> scan(ScanEnhancedRequest scanEnhancedRequest, Class<T> clazz) {
		return prepareTable(clazz).scan(scanEnhancedRequest);
	}

	public <T> PageIterable<T> scanAll(Class<T> clazz) {
		return prepareTable(clazz).scan();
	}

	public <T> PageIterable<T> query(QueryEnhancedRequest queryEnhancedRequest, Class<T> clazz) {
		return prepareTable(clazz).query(queryEnhancedRequest);
	}

	private <T> DynamoDbTable<T> prepareTable(T entity) {
		String tableName = dynamoDbTableNameResolver.resolve(entity.getClass());
		return dynamoDbEnhancedClient.table(tableName,
				dynamoDbTableSchemaResolver.resolve(entity.getClass(), tableName));
	}

	private <T> DynamoDbTable<T> prepareTable(Class<T> clazz) {
		String tableName = dynamoDbTableNameResolver.resolve(clazz);
		return dynamoDbEnhancedClient.table(tableName, dynamoDbTableSchemaResolver.resolve(clazz, tableName));
	}

}
