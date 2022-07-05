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
package io.awspring.cloud.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;

/**
 * Tests for {@link SpringCloudClientConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class SpringCloudClientConfigurationTests {

	@Test
	void returnsClientConfigurationWithVersion() {
		ClientOverrideConfiguration clientOverrideConfiguration = new SpringCloudClientConfiguration()
				.clientOverrideConfiguration();

		assertThat(clientOverrideConfiguration.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX)).isPresent()
				.hasValueSatisfying(value -> {
					assertThat(value).startsWith("spring-cloud-aws");
				});
	}
}
