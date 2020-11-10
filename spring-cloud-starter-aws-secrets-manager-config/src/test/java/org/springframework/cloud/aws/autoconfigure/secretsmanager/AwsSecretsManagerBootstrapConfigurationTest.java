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

package org.springframework.cloud.aws.autoconfigure.secretsmanager;

import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AwsSecretsManagerBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AwsSecretsManagerBootstrapConfiguration.class));

	@Test
	void testWithStaticRegion() {
		this.contextRunner.withPropertyValues("aws.secretsmanager.enabled:true", "aws.secretsmanager.region:us-east-2")
				.run(context -> {
					assertThat(context).hasSingleBean(AwsSecretsManagerPropertySourceLocator.class);
					assertThat(context).hasSingleBean(AWSSecretsManagerClient.class);
					assertThat(context).hasSingleBean(AWSSecretsManager.class);
					assertThat(context).hasSingleBean(AwsSecretsManagerProperties.class);

					AwsSecretsManagerProperties awsSecretsManagerProperties = context
							.getBean(AwsSecretsManagerProperties.class);
					AWSSecretsManagerClient awsSecretsManagerClient = context.getBean(AWSSecretsManagerClient.class);

					Method signingRegionMethod = ReflectionUtils.findMethod(AmazonWebServiceClient.class,
							"getSigningRegion");
					signingRegionMethod.setAccessible(true);
					String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod,
							awsSecretsManagerClient);

					assertThat(signedRegion).isEqualTo(awsSecretsManagerProperties.getRegion());
				});
	}

	@Test
	void testUserAgent() {
		this.contextRunner.withPropertyValues("aws.secretsmanager.region:us-east-2").run(context -> {
			assertThat(context.getBean(AWSSecretsManagerClient.class).getClientConfiguration().getUserAgentSuffix())
					.startsWith("spring-cloud-aws/");
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
