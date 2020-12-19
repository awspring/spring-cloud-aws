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

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bernardo Martins
 */
class OnAwsCloudEnvironmentConditionTest {

	@Test
	void condition_methodAnnotation_shouldNotCreateBeanFoo() {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithConditionalOnAwsCloudEnvironmentAnnotatedBean.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isFalse();
	}

	@Test
	void condition_typeAnnotation_shouldNotCreateBeanFoo() {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithConditionalOnAwsCloudEnvironmentAnnotation.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isFalse();
	}

	@Test
	void condition_methodAnnotation_shouldCreateBeanFoo() {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithConditionalOnMissingAwsCloudEnvironmentAnnotatedBean.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isTrue();
	}

	@Test
	void condition_typeAnnotation_shouldCreateBeanFoo() {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigWithConditionalOnMissingAwsCloudEnvironmentAnnotation.class);

		// Assert
		assertThat(applicationContext.containsBean("foo")).isTrue();
	}

	@Configuration
	protected static class ConfigWithConditionalOnAwsCloudEnvironmentAnnotatedBean {

		@Bean
		@ConditionalOnAwsCloudEnvironment
		String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnAwsCloudEnvironment
	protected static class ConfigWithConditionalOnAwsCloudEnvironmentAnnotation {

		@Bean
		String foo() {
			return "foo";
		}

	}

	@Configuration
	protected static class ConfigWithConditionalOnMissingAwsCloudEnvironmentAnnotatedBean {

		@Bean
		@ConditionalOnMissingAwsCloudEnvironment
		String foo() {
			return "foo";
		}

	}

	@Configuration
	@ConditionalOnMissingAwsCloudEnvironment
	protected static class ConfigWithConditionalOnMissingAwsCloudEnvironmentAnnotation {

		@Bean
		String foo() {
			return "foo";
		}

	}

}
