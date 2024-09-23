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
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

public class ConfiguredAwsPresigner {

	private final AttributeMap attributes;
	private final SdkPresigner presigner;

	public ConfiguredAwsPresigner(SdkPresigner presigner) {
		this.presigner = presigner;
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(presigner,
				"clientConfiguration");
		this.attributes = (AttributeMap) ReflectionTestUtils.getField(Objects.requireNonNull(clientConfiguration),
				"attributes");
	}

	public URI getEndpoint() {
		return attributes.get(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint();
	}

	public boolean isEndpointOverridden() {
		return attributes.get(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden();
	}

	public Boolean getFipsEnabled() {
		return (Boolean) ReflectionTestUtils.getField(presigner, "fipsEnabled");
	}

	public Boolean getDualstackEnabled() {
		return (Boolean) ReflectionTestUtils.getField(presigner, "dualstackEnabled");
	}

	public Region getRegion() {
		return (Region) ReflectionTestUtils.getField(presigner, "region");
	}
}
