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

package org.springframework.cloud.aws.autoconfigure.mail;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

class SimpleEmailAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SimpleEmailAutoConfiguration.class));

	@Test
	void mailSender_MailSenderWithJava_configuresJavaMailSender() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isSameAs(context.getBean(MailSender.class));

			AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);

			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.DEFAULT_REGION.getName());
		});
	}

	@Test
	void enableAutoConfigurationWithSpecificRegion() {
		this.contextRunner.withPropertyValues("cloud.aws.mail.region:us-east-1").run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isSameAs(context.getBean(MailSender.class));

			AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);

			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.US_EAST_1.getName());
		});
	}

	@Test
	void mailSenderWithSimpleEmail() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("javax.mail.Session")).run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void mailIsDisabled() {
		this.contextRunner.withPropertyValues("cloud.aws.mail.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(MailSender.class);
			assertThat(context).doesNotHaveBean(JavaMailSender.class);
		});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(SesAutoConfigurationTest.ConfigurationWithGlobalClientConfiguration.class)
				.run((context) -> {
					AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("global");
				});
	}

	@Test
	void configuration_withSesClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SesAutoConfigurationTest.ConfigurationWithSesClientConfiguration.class)
				.run((context) -> {
					AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("ses");
				});
	}

	@Test
	void configuration_withGlobalAndSesClientConfigurations_shouldUseSqsConfigurationForClient() {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(SesAutoConfigurationTest.ConfigurationWithGlobalAndSesClientConfiguration.class)
				.run((context) -> {
					AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("ses");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalClientConfiguration {

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithSesClientConfiguration {

		@Bean
		ClientConfiguration sesClientConfiguration() {
			return new ClientConfiguration().withProxyHost("ses");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalAndSesClientConfiguration {

		@Bean
		ClientConfiguration sesClientConfiguration() {
			return new ClientConfiguration().withProxyHost("ses");
		}

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

}
