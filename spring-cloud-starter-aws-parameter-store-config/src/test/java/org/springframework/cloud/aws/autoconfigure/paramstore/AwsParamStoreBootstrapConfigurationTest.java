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
import java.net.URI;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.paramstore.AwsParamStoreProperties;
import org.springframework.cloud.aws.paramstore.AwsParamStorePropertySourceLocator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link AwsParamStoreBootstrapConfiguration}.
 *
 * @author Matej Nedic
 * @author Eddú Meléndez
 */
class AwsParamStoreBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AwsParamStoreBootstrapConfiguration.class));

	@Test
	void testWithStaticRegion() {
		this.contextRunner.withPropertyValues("aws.paramstore.enabled:true", "aws.paramstore.region:us-east-2")
				.run(context -> {
					assertThat(context).hasSingleBean(AwsParamStorePropertySourceLocator.class);
					assertThat(context).hasSingleBean(AWSSimpleSystemsManagement.class);
					assertThat(context).hasSingleBean(AWSSimpleSystemsManagementClient.class);
					assertThat(context).hasSingleBean(AwsParamStoreProperties.class);

					AwsParamStoreProperties awsParamStoreProperties = context.getBean(AwsParamStoreProperties.class);
					AWSSimpleSystemsManagementClient awsSimpleClient = context
							.getBean(AWSSimpleSystemsManagementClient.class);

					Method signingRegionMethod = ReflectionUtils.findMethod(AmazonWebServiceClient.class,
							"getSigningRegion");
					signingRegionMethod.setAccessible(true);
					String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod, awsSimpleClient);

					assertThat(signedRegion).isEqualTo(awsParamStoreProperties.getRegion());
				});
	}

	@Test
	void testWithCustomEndpoint() {
		contextRunner.withPropertyValues("aws.paramstore.endpoint:http://localhost:8090").run((context) -> {
			AWSSimpleSystemsManagementClient client = context.getBean(AWSSimpleSystemsManagementClient.class);
			Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
			assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

			Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client, "isEndpointOverridden");
			assertThat(isEndpointOverridden).isTrue();
		});
	}

	@Test
	void testMissingAutoConfiguration() {
		this.contextRunner.withPropertyValues("aws.paramstore.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(AwsParamStorePropertySourceLocator.class);
			assertThat(context).doesNotHaveBean(AWSSimpleSystemsManagement.class);
		});
	}

	@Test
	void testUserAgent() {
		this.contextRunner.withPropertyValues("aws.paramstore.region:us-east-2").run(context -> {
			assertThat(context.getBean(AWSSimpleSystemsManagementClient.class).getClientConfiguration()
					.getUserAgentSuffix()).startsWith("spring-cloud-aws/");
		});

	}

}
