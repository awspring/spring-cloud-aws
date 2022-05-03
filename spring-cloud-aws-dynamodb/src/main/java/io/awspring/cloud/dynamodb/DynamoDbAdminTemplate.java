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
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;

public class DynamoDbAdminTemplate implements DynamoDbAdminOperations {
	private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
	private final DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver;
	private final DynamoDbTableNameResolver dynamoDbTableNameResolver;

	public DynamoDbAdminTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient, DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver, DynamoDbTableNameResolver dynamoDbTableNameResolver) {
		this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
		this.dynamoDbTableSchemaResolver = dynamoDbTableSchemaResolver;
		this.dynamoDbTableNameResolver = dynamoDbTableNameResolver;
	}

	@Override
	public <T> void createTable(Class<T> clazz){
		prepareTable(clazz).createTable();
	}

	@Override
	public <T> DescribeTableEnhancedResponse describeTable(Class<T> clazz) {
		return prepareTable(clazz).describeTable();
	}

	@Override
	public <T> void deleteTable(Class<T> clazz) {
		prepareTable(clazz).deleteTable();
	}

	private <T> DynamoDbTable<T> prepareTable(Class<T> clazz) {
		String tableName = dynamoDbTableNameResolver.resolve(clazz);
		return dynamoDbEnhancedClient.table(tableName, dynamoDbTableSchemaResolver.resolve(clazz, tableName));
	}
}
