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

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Resolving table schema and table name from a class.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 * @since 3.0
 */
public interface DynamoDbTableSchemaResolver {

	/**
	 * Resolving {@link TableSchema} from {@link Class}.
	 *
	 * @param clazz - the class from which table schema is resolved
	 * @return table schema
	 * @param <T> - type
	 */
	<T> TableSchema<T> resolve(Class<T> clazz);
}
