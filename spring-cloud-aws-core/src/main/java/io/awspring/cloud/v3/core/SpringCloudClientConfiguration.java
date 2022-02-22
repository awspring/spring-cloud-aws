/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.core;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;

/**
 * @author Eddú Meléndez
 */
public final class SpringCloudClientConfiguration {

	private static final String NAME = "spring-cloud-aws";

	private static final String VERSION = "3.0";

	private SpringCloudClientConfiguration() {

	}

	private static String getUserAgent() {
		return NAME + "/" + VERSION;
	}

	public static ClientOverrideConfiguration clientOverrideConfiguration() {
		return ClientOverrideConfiguration.builder()
				.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, getUserAgent()).build();
	}

}
