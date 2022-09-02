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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Default implementation with simple cache for {@link TableSchema}.
 *
 * @author Matej Nedic
 */
public class DefaultDynamoDbTableSchemaResolver implements DynamoDbTableSchemaResolver {
	private final Map<String, TableSchema> tableSchemaCache = new ConcurrentHashMap<>();

	@Override
	public <T> TableSchema<T> resolve(Class<T> clazz, String tableName) {
		return tableSchemaCache.computeIfAbsent(tableName, entityClassName -> TableSchema.fromBean(clazz));
	}
}
