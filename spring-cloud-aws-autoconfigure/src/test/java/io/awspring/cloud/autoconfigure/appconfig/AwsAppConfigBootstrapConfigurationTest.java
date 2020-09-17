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

package io.awspring.cloud.autoconfigure.appconfig;

import java.lang.reflect.Method;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import com.amazonaws.services.appconfig.AmazonAppConfigClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AwsAppConfigBootstrapConfigurationTest {

	private ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(AwsAppConfigBootstrapConfiguration.class);

	private static String[] properties = new String[] { "aws.appconfig.region=us-east-2",
			"aws.appconfig.account-id=1234567", "aws.appconfig.application=demo",
			"aws.appconfig.environment=dev" };

	@Test
	void testWithStaticRegion() {
		Method signingRegionMethod = ReflectionUtils
				.findMethod(AmazonAppConfigClient.class, "getSigningRegion");
		signingRegionMethod.setAccessible(true);

		runner.withPropertyValues(properties).run(context -> {
			AmazonAppConfig appConfig = context.getBean(AmazonAppConfig.class);

			assertThat(appConfig).isNotNull();

			assertThat(ReflectionUtils.invokeMethod(signingRegionMethod, appConfig))
					.isEqualTo("us-east-2");
		});
	}

	@Test
	void testUserAgent() {
		runner.withPropertyValues(properties)
				.run(context -> assertThat(context.getBean(AmazonAppConfig.class))
						.isNotNull().extracting(AmazonAppConfigClient.class::cast)
						.extracting(appconfig -> appconfig.getClientConfiguration()
								.getUserAgentSuffix())
						.asString().startsWith("spring-cloud-aws/"));
	}

}
