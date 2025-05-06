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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple implementation of {@link DynamoDbTableNameResolver} that resolves class simple name to table name.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @author Volodymyr Ivakhnenko
 * @since 3.0
 */
public class DefaultDynamoDbTableNameResolver implements DynamoDbTableNameResolver {

	@Nullable
	private final String tablePrefix;

	@Nullable
	private final String tableSuffix;

	@Nullable
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
		this.tablePrefix = tablePrefix;
		this.tableSuffix = tableSuffix;
		this.tableSeparator = tableSeparator;
	}

	@Override
	public String resolve(Class clazz) {
		Assert.notNull(clazz, "clazz is required");

		String prefix = StringUtils.hasText(tablePrefix) ? tablePrefix : "";
		String suffix = StringUtils.hasText(tableSuffix) ? tableSuffix : "";
		String separator = StringUtils.hasText(tableSeparator) ? tableSeparator : "_";

		return prefix
			.concat(clazz.getSimpleName().replaceAll("(.)(\\p{Lu})", "$1" + separator + "$2").toLowerCase(Locale.ROOT))
			.concat(suffix);
	}

}
