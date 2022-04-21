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
public class DynamoDBTemplate implements DynamoDBOperations {
	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final TableSchemaResolver tableSchemaResolver;

	public DynamoDBTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient, TableSchemaResolver tableSchemaResolver) {
		this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
		this.tableSchemaResolver = tableSchemaResolver;
	}

	public <T extends TableNameProvider> T save(T entity) {
		prepareTable(entity).putItem(entity);
		return entity;
	}

	public <T extends TableNameProvider> T update(T entity) {
		return prepareTable(entity).updateItem(entity);
	}

	public void delete(Key key, Class<?> clazz, String tableName) {
		prepareTable(clazz, tableName).deleteItem(key);
	}

	public <T extends TableNameProvider> void delete(T entity) {
		prepareTable(entity).deleteItem(entity);
	}

	public <T> T load(Key key, Class<T> clazz, String tableName) {
		return prepareTable(clazz, tableName).getItem(key);
	}

	public <T> PageIterable<T> scan(ScanEnhancedRequest scanEnhancedRequest, Class<T> clazz, String tableName ) {
		return prepareTable(clazz, tableName).scan(scanEnhancedRequest);
	}

	public <T> PageIterable<T> scanAll(Class<T> clazz, String tableName) {
		return prepareTable(clazz, tableName).scan();
	}

	public <T> PageIterable<T> query(QueryEnhancedRequest queryEnhancedRequest, Class<T> clazz, String tableName) {
		return prepareTable(clazz, tableName).query(queryEnhancedRequest);
	}

	private <T extends TableNameProvider> DynamoDbTable<T> prepareTable(T entity){
	 return dynamoDbEnhancedClient.table(entity.getTableName(), tableSchemaResolver.resolve(entity.getClass(), entity.getTableName()));
	}

	private <T> DynamoDbTable<T> prepareTable(Class<T> clazz, String tableName) {
		return dynamoDbEnhancedClient.table(tableName, tableSchemaResolver.resolve(clazz, tableName));
	}

}
