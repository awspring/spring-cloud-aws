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
package io.awspring.cloud.autoconfigure;

import java.net.URI;
import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.utils.AttributeMap;

public class ConfiguredAwsClient {

	private final AttributeMap clientConfigurationAttributes;

	public ConfiguredAwsClient(SdkClient sdkClient) {
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(sdkClient,
				"clientConfiguration");
		this.clientConfigurationAttributes = (AttributeMap) ReflectionTestUtils
				.getField(Objects.requireNonNull(clientConfiguration), "attributes");
	}

	public URI getEndpoint() {
		return clientConfigurationAttributes.get(SdkClientOption.ENDPOINT);
	}

	public boolean isEndpointOverridden() {
		return clientConfigurationAttributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN);
	}

}
