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

import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.presigner.SdkPresigner;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public class ConfiguredAwsPresigner {

	private final AttributeMap presignerConfigurationAttributes;

	public ConfiguredAwsPresigner(SdkPresigner sdkPresigner) {
		SdkClientConfiguration clientConfiguration;
		try {
			clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(sdkPresigner,
					"clientConfiguration");
		}
		catch (IllegalArgumentException e) {
			// special case for S3CrtAsyncClient
			Object delegate = ReflectionTestUtils.getField(sdkPresigner, "delegate");
			clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(delegate,
					"clientConfiguration");
		}
		this.presignerConfigurationAttributes = (AttributeMap) ReflectionTestUtils
				.getField(Objects.requireNonNull(clientConfiguration), "attributes");
	}

	public URI getEndpoint() {
		return presignerConfigurationAttributes.get(SdkClientOption.ENDPOINT);
	}

	public boolean isEndpointOverridden() {
		return presignerConfigurationAttributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN);
	}

	public Region getRegion() {
		return presignerConfigurationAttributes.get(AwsClientOption.AWS_REGION);
	}

	public Duration getApiCallTimeout() {
		return presignerConfigurationAttributes.get(SdkClientOption.API_CALL_TIMEOUT);
	}

	public SdkHttpClient getSyncHttpClient() {
		return presignerConfigurationAttributes.get(SdkClientOption.SYNC_HTTP_CLIENT);
	}

	public Boolean getFipsEnabled() {
		return presignerConfigurationAttributes.get(AwsClientOption.FIPS_ENDPOINT_ENABLED);
	}

	public Boolean getDualstackEnabled() {
		return presignerConfigurationAttributes.get(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED);
	}

	public DefaultsMode getDefaultsMode() {
		return presignerConfigurationAttributes.get(AwsClientOption.DEFAULTS_MODE);
	}

	public SdkAsyncHttpClient getAsyncHttpClient() {
		return presignerConfigurationAttributes.get(SdkClientOption.ASYNC_HTTP_CLIENT);
	}

}
