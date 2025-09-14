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

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Simple implementation of {@link DynamoDbTableNameResolver} that resolves class simple name to table name.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @author Volodymyr Ivakhnenko
 * @author Marcus Voltolim
 * @since 3.0
 */
public class DefaultDynamoDbTableNameResolver implements DynamoDbTableNameResolver {

	private final Map<Class<?>, String> tableNameCache = new ConcurrentHashMap<>();

	private final String tablePrefix;
	private final String tableSuffix;
	private final String tableSeparator;

	public DefaultDynamoDbTableNameResolver() {
		this(null, null, null);
	}

	public DefaultDynamoDbTableNameResolver(@Nullable String tablePrefix) {
		this(tablePrefix, null, null);
	}

	public DefaultDynamoDbTableNameResolver(@Nullable String tablePrefix, @Nullable String tableSuffix) {
		this(tablePrefix, tableSuffix, null);
	}

	public DefaultDynamoDbTableNameResolver(@Nullable String tablePrefix, @Nullable String tableSuffix,
			@Nullable String tableSeparator) {
		this.tablePrefix = StringUtils.hasText(tablePrefix) ? tablePrefix : "";
		this.tableSuffix = StringUtils.hasText(tableSuffix) ? tableSuffix : "";
		this.tableSeparator = StringUtils.hasText(tableSeparator) ? tableSeparator : "_";
	}

	@Override
	public <T> String resolve(Class<T> clazz) {
		return tableNameCache.computeIfAbsent(clazz, this::resolveInternal);
	}

	private <T> String resolveInternal(Class<T> clazz) {
		final String className = clazz.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1" + tableSeparator + "$2");
		return tablePrefix + className.toLowerCase(Locale.ROOT) + tableSuffix;
	}

	Map<Class<?>, String> getTableNameCache() {
		return tableNameCache;
	}

}
