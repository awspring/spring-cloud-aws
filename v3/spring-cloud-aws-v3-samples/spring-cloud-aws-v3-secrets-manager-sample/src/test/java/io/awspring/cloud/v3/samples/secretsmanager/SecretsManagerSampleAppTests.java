/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.samples.secretsmanager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SecretsManagerSampleApp}. This test runs with the secrets
 * manager integration turned off. If you want to run this test with *real* integration
 * with AWS Secrets manager, you need to switch on the spring.cloud.aws.secretsmanager:
 * true in the application.yml. Also you would need to create the required secret in AWS
 * using the AWS Management Console or aws-cli.
 *
 * This test is for demonstrating the fact, that the {@link SecretsManagerSampleApp}
 * sample loads up correctly and the Spring Cloud AWS Secrets Manager auto-configuration
 * works.
 *
 * @author Arun Patra
 */
@SpringBootTest
public class SecretsManagerSampleAppTests {

	@Autowired
	private Environment env;

	/**
	 * Tests the application loads correctly.
	 */
	@Test
	void contextLoads() {
		assertThat(env.containsProperty("password")).isTrue();

		// If you switch on the Secrets Manager integration, you should change the
		// assertion below to compare against
		// the actual value you of `password` you have configured in AWS.
		assertThat(env.getProperty("password")).isEqualTo("localpassword");
	}

}
