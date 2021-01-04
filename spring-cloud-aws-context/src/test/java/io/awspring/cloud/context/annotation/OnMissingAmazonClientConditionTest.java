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

package io.awspring.cloud.context.annotation;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
		AmazonS3 amazonS3() {
			return mock(AmazonS3.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class ConfigWithMissingAmazonClientCondition {

		@Bean
		@ConditionalOnMissingAmazonClient(AmazonS3.class)
		String foo() {
			return "foo";
		}

	}

}
