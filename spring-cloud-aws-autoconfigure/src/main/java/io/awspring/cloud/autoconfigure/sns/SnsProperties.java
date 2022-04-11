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
package io.awspring.cloud.autoconfigure.sns;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties related to AWS SNS.
 *
 * @author Matej Nedic
 * @since 3.0
 */
@ConfigurationProperties(prefix = SnsProperties.PREFIX)
public class SnsProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS SNS configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.sns";

	/**
	 * Value which determines will auto create for topic creation be used when {@link SnsTemplate} methods are called.
	 */
	private boolean autoCreate = true;

	public boolean getAutoCreate() {
		return autoCreate;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

}
