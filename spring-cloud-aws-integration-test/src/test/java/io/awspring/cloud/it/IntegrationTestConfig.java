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

package org.springframework.cloud.aws.it;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;

import org.springframework.cloud.aws.context.config.annotation.EnableContextCredentials;
import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration;
import org.springframework.cloud.aws.it.support.TestStackEnvironment;
import org.springframework.cloud.aws.it.support.TestStackInstanceIdService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;

/**
 * @author Agim Emruli
 */
@Configuration
@EnableContextCredentials(accessKey = "${aws-integration-tests.access-key}",
		secretKey = "${aws-integration-tests.secret-key}")
@EnableStackConfiguration(stackName = "IntegrationTestStack")
@PropertySource("classpath:application.properties")
@EnableContextRegion(region = "eu-west-1")
@Order(1)
public class IntegrationTestConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer configurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public TestStackEnvironment testStackEnvironment(AmazonCloudFormation amazonCloudFormation) {
		return new TestStackEnvironment(amazonCloudFormation);
	}

	@Bean
	public TestStackInstanceIdService testStackInstanceIdService(AmazonCloudFormation amazonCloudFormation) {
		return TestStackInstanceIdService.fromStackOutputKey(TestStackEnvironment.DEFAULT_STACK_NAME,
				TestStackEnvironment.INSTANCE_ID_STACK_OUTPUT_KEY, amazonCloudFormation);
	}

}
