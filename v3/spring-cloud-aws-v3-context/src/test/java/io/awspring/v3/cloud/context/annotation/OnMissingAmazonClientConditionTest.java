/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.v3.cloud.context.annotation;


import io.awspring.cloud.v3.context.annotation.ConditionalOnMissingAmazonClient;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
class OnMissingAmazonClientConditionTest {

	@Test
	void condition_withMatchingCase_shouldCreateBeanFoo() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithMissingAmazonClientCondition.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isTrue();
	}

	@Test
	void condition_withNonMatchingCase_shouldNotCreateBeanFoo() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithDummyS3Client.class, ConfigWithMissingAmazonClientCondition.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isFalse();
	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConfigWithDummyS3Client {

		@Bean
		S3Client amazonS3() {
			return mock(S3Client.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConfigWithMissingAmazonClientCondition {

		@Bean
		@ConditionalOnMissingAmazonClient(S3Client.class)
		String foo() {
			return "foo";
		}

	}

}
