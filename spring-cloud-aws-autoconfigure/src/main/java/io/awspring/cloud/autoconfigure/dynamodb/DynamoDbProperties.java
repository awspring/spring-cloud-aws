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
package io.awspring.cloud.autoconfigure.dynamodb;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.lang.Nullable;

/**
 * Properties related to AWS DynamoDB.
 * @author Matej Nedic
 * @author Arun Patra
 * @since 3.0.0
 */
@ConfigurationProperties(prefix = DynamoDbProperties.PREFIX)
public class DynamoDbProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.dynamodb";

	/**
	 * The prefix used to resolve table names.
	 */
	@Nullable
	private String tablePrefix;

	/**
	 * The suffix used to resolve table names.
	 */
	@Nullable
	private String tableSuffix;

	/**
	 * The word separator used to resolve table names.
	 */
	@Nullable
	private String tableSeparator;

	/**
	 * Properties that are used to configure {@link software.amazon.dax.ClusterDaxClient}.
	 */
	@NestedConfigurationProperty
	private DaxProperties dax = new DaxProperties();

	public String getTablePrefix() {
		return tablePrefix;
	}

	public String getTableSuffix() {
		return tableSuffix;
	}

	public String getTableSeparator() {
		return tableSeparator;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setTableSuffix(String tableSuffix) {
		this.tableSuffix = tableSuffix;
	}

	public void setTableSeparator(String tableSeparator) {
		this.tableSeparator = tableSeparator;
	}

	public DaxProperties getDax() {
		return dax;
	}

	public void setDax(DaxProperties dax) {
		this.dax = dax;
	}
}
