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

package org.springframework.cloud.aws.autoconfigure.paramstore;

import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.aws.paramstore.AwsParamStoreProperties;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link AwsParamStoreBootstrapConfiguration}.
 *
 * @author Matej Nedic
 * @author Eddú Meléndez
 */
class AwsParamStoreBootstrapConfigurationTest {

	AwsParamStoreBootstrapConfiguration bootstrapConfig = new AwsParamStoreBootstrapConfiguration();

	@Test
	void testWithStaticRegion() {
		String region = "us-east-2";
		AWSSimpleSystemsManagementClient awsSimpleClient = createParamStoreClient(region);

		Method signingRegionMethod = ReflectionUtils
				.findMethod(AmazonWebServiceClient.class, "getSigningRegion");
		signingRegionMethod.setAccessible(true);
		String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod,
				awsSimpleClient);

		assertThat(signedRegion).isEqualTo(region);
	}

	@Test
	void testUserAgent() {
		String region = "us-east-2";
		AWSSimpleSystemsManagementClient awsSimpleClient = createParamStoreClient(region);

		assertThat(awsSimpleClient.getClientConfiguration().getUserAgentSuffix())
				.startsWith("spring-cloud-aws/");
	}

	private AWSSimpleSystemsManagementClient createParamStoreClient(String region) {
		AwsParamStoreProperties awsParamStoreProperties = new AwsParamStoreProperties();
		awsParamStoreProperties.setRegion(region);

		Method SSMClientMethod = ReflectionUtils.findMethod(
				AwsParamStoreBootstrapConfiguration.class, "ssmClient",
				AwsParamStoreProperties.class);
		SSMClientMethod.setAccessible(true);
		AWSSimpleSystemsManagementClient awsSimpleClient = (AWSSimpleSystemsManagementClient) ReflectionUtils
				.invokeMethod(SSMClientMethod, bootstrapConfig, awsParamStoreProperties);
		return awsSimpleClient;
	}

}
