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
package io.awspring.cloud.autoconfigure.ses;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Properties related to AWS Simple Email Service.
 *
 * @author Eddú Meléndez
 * @author Arun Patra
 */
@ConfigurationProperties(prefix = SesProperties.PREFIX)
public class SesProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.ses";

	/**
	 * Configures source ARN. Used only for sending authorization.
	 */
	@Nullable
	private String sourceArn;

	/**
	 * Configures configuration set name.
	 */
	@Nullable
	private String configurationSetName;

	@Nullable
	public String getSourceArn() {
		return sourceArn;
	}

	@Nullable
	public String getConfigurationSetName() {
		return configurationSetName;
	}

	public void setSourceArn(@Nullable String sourceArn) {
		this.sourceArn = sourceArn;
	}

	public void setConfigurationSetName(@Nullable String configurationSetName) {
		this.configurationSetName = configurationSetName;
	}
}
