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

package io.awspring.cloud.autoconfigure.mail;

import com.amazonaws.ClientConfiguration;
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

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class SesAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SesAutoConfiguration.class));

	@Test
	void mailSenderWithJavaMail() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isNotNull();
			assertThat(context.getBean(JavaMailSender.class)).isSameAs(context.getBean(MailSender.class));
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
	void mailSenderWithDefaultRegion() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("javax.mail.Session")).run(context -> {
			assertThat(context.getBean(MailSender.class)).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isNotNull();
			assertThat(context.getBean("simpleMailSender")).isSameAs(context.getBean(MailSender.class));

			AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);
			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo("us-west-2");
		});
	}

	@Test
	void mailSenderWithCustomRegion() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.region:us-east-1")
				.withClassLoader(new FilteredClassLoader("javax.mail.Session")).run(context -> {
					assertThat(context.getBean(MailSender.class)).isNotNull();
					assertThat(context.getBean("simpleMailSender")).isNotNull();
					assertThat(context.getBean("simpleMailSender")).isSameAs(context.getBean(MailSender.class));

					AmazonSimpleEmailServiceClient client = context.getBean(AmazonSimpleEmailServiceClient.class);
					Object region = ReflectionTestUtils.getField(client, "signingRegion");
					assertThat(region).isEqualTo("us-east-1");
				});
	}

	@Test
	void sesAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(MailSender.class);
			assertThat(context).doesNotHaveBean(JavaMailSender.class);
		});
	}

	@Test
	void sesAutoConfigurationIsDisabledButSimpleEmailAutoConfigurationIsEnabled() {
		new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(SesAutoConfiguration.class, SimpleEmailAutoConfiguration.class))
				.withPropertyValues("spring.cloud.aws.ses.enabled:false").run(context -> {
					assertThat(context).hasSingleBean(MailSender.class);
					assertThat(context).hasSingleBean(JavaMailSender.class);
				});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalClientConfiguration.class).run((context) -> {
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
		this.contextRunner.withUserConfiguration(ConfigurationWithSesClientConfiguration.class).run((context) -> {
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
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalAndSesClientConfiguration.class)
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
