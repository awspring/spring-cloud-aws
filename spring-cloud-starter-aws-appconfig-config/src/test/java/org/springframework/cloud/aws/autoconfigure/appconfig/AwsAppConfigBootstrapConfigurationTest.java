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

package org.springframework.cloud.aws.autoconfigure.appconfig;

import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.appconfig.AmazonAppConfigClient;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.aws.appconfig.AwsAppConfigProperties;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsAppConfigBootstrapConfigurationTest {

	AwsAppConfigBootstrapConfiguration bootstrapConfig = new AwsAppConfigBootstrapConfiguration();

	@Test
	void testWithStaticRegion() {
		String region = "us-east-2";
		AmazonAppConfigClient appConfigClient = createAppConfigAsync(region);

		Method signingRegionMethod = ReflectionUtils
				.findMethod(AmazonWebServiceClient.class, "getSigningRegion");

		signingRegionMethod.setAccessible(true);

		String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod,
				appConfigClient);

		assertThat(signedRegion).isEqualTo(region);
	}

	@Test
	void testUserAgent() {
		String region = "us-east-2";
		AmazonAppConfigClient appConfigClient = createAppConfigAsync(region);

		assertThat(appConfigClient.getClientConfiguration().getUserAgentSuffix())
				.startsWith("spring-cloud-aws/");
	}

	private AmazonAppConfigClient createAppConfigAsync(String region) {
		AwsAppConfigProperties awsAppConfigProperties = new AwsAppConfigProperties();
		awsAppConfigProperties.setRegion(region);

		Method appConfigAsync = ReflectionUtils.findMethod(
				AwsAppConfigBootstrapConfiguration.class, "appConfigClient",
				AwsAppConfigProperties.class);

		appConfigAsync.setAccessible(true);

		AmazonAppConfigClient appConfigClient = (AmazonAppConfigClient) ReflectionUtils
				.invokeMethod(appConfigAsync, bootstrapConfig, awsAppConfigProperties);

		return appConfigClient;
	}

}
