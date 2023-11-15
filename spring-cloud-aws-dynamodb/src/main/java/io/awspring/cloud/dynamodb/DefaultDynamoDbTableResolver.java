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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Default implementation with simple cache for {@link TableSchema}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public class DefaultDynamoDbTableResolver implements DynamoDbTableResolver {
	private final DynamoDbTableNameResolver tableNameResolver;
	private final List<TableSchema<?>> tableSchemas;
	private final Map<String, TableSchema> tableSchemaCache = new ConcurrentHashMap<>();

	DefaultDynamoDbTableResolver() {
		this(new DefaultDynamoDbTableNameResolver(), Collections.emptyList());
	}

	public DefaultDynamoDbTableResolver(DynamoDbTableNameResolver tableNameResolver,
			List<TableSchema<?>> tableSchemas) {
		this.tableNameResolver = tableNameResolver;
		this.tableSchemas = tableSchemas;
	}

	public <T> TableSchema<T> resolveTableSchema(Class<T> clazz) {
		String tableName = tableNameResolver.resolve(clazz);
		return tableSchemaCache.computeIfAbsent(tableName,
				entityClassName -> tableSchemas.stream().filter(it -> it.itemType().rawClass().equals(clazz))
						.findFirst().orElseGet(() -> TableSchema.fromBean(clazz)));
	}

	@Override
	public String resolveTableName(Class<?> aClass) {
		return tableNameResolver.resolve(aClass);
	}

	Map<String, TableSchema> getTableSchemaCache() {
		return tableSchemaCache;
	}
}
