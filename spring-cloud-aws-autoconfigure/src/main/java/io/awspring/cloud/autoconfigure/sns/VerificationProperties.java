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
package io.awspring.cloud.autoconfigure.sns;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * @author kazaff
 */
@ConfigurationProperties(prefix = VerificationProperties.CONFIG_PREFIX)
public class VerificationProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws.sns.verification";

	@Nullable
	private String enabled;

	@Nullable
	private long maxLifeTimeOfMessage;

	@Nullable
	public String getEnabled() {
		return enabled;
	}

	public void setEnabled(@Nullable String enabled) {
		this.enabled = enabled;
	}

	public long getMaxLifeTimeOfMessage() {
		return maxLifeTimeOfMessage;
	}

	public void setMaxLifeTimeOfMessage(long maxLifeTimeOfMessage) {
		this.maxLifeTimeOfMessage = maxLifeTimeOfMessage;
	}
}
