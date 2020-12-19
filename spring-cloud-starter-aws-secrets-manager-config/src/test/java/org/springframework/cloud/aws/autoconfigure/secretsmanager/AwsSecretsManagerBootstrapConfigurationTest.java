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

package io.awspring.cloud.autoconfigure.secretsmanager;

import java.lang.reflect.Method;
import java.net.URI;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import io.awspring.cloud.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AwsSecretsManagerBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AwsSecretsManagerBootstrapConfiguration.class));

	@Test
	void testWithStaticRegion() {
		contextRunner.withPropertyValues("aws.secretsmanager.region:us-east-2").run((context) -> {
			AWSSecretsManagerClient client = context.getBean(AWSSecretsManagerClient.class);
			Method signingRegionMethod = ReflectionUtils.findMethod(AmazonWebServiceClient.class, "getSigningRegion");
			signingRegionMethod.setAccessible(true);
			String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod, client);

			assertThat(signedRegion).isEqualTo("us-east-2");
		});
	}

	@Test
	void testWithCustomEndpoint() {
		contextRunner.withPropertyValues("aws.secretsmanager.endpoint:http://localhost:8090").run((context) -> {
			AWSSecretsManagerClient client = context.getBean(AWSSecretsManagerClient.class);
			Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
			assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

			Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client, "isEndpointOverridden");
			assertThat(isEndpointOverridden).isTrue();
		});
	}

	@Test
	void testUserAgent() {
		contextRunner.withPropertyValues("aws.secretsmanager.region:us-east-2").run((context) -> {
			AWSSecretsManagerClient client = context.getBean(AWSSecretsManagerClient.class);
			assertThat(client.getClientConfiguration().getUserAgentSuffix()).startsWith("spring-cloud-aws/");
		});
	}

	@Test
	void testMissingAutoConfiguration() {
		this.contextRunner.withPropertyValues("aws.secretsmanager.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(AwsSecretsManagerPropertySourceLocator.class);
			assertThat(context).doesNotHaveBean(AWSSecretsManager.class);
		});
	}

}
