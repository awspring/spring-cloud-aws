/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.ses;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientConfigurer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

/**
 * Tests for class {@link SesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
class SesAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SesAutoConfiguration.class));

	@Test
	void mailSenderWithJavaMail() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MailSender.class);
			assertThat(context).hasSingleBean(JavaMailSender.class);
			assertThat(context).getBean(JavaMailSender.class).isSameAs(context.getBean(MailSender.class));
		});
	}

	@Test
	void mailSenderWithoutSesClientInTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(software.amazon.awssdk.services.ses.SesClient.class))
				.run(context -> {
					assertThat(context).doesNotHaveBean(MailSender.class);
					assertThat(context).doesNotHaveBean(JavaMailSender.class);
				});
	}

	@Test
	void mailSenderWithSimpleEmail() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(javax.mail.Session.class)).run(context -> {
			assertThat(context).hasSingleBean(MailSender.class);
			assertThat(context).hasBean("simpleMailSender");
			assertThat(context).getBean("simpleMailSender").isSameAs(context.getBean(MailSender.class));
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
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ses.endpoint:http://localhost:8090").run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(client.isEndpointOverridden()).isTrue();
		});
	}

	@Test
	void withCustomGlobalEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(client.isEndpointOverridden()).isTrue();
		});
	}

	@Test
	void withCustomGlobalEndpointAndSesEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
				"spring.cloud.aws.ses.endpoint:http://localhost:9999").run(context -> {
					ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SesClient.class));
					assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
					assertThat(client.isEndpointOverridden()).isTrue();
				});
	}

	@Test
	void customSesClientConfigurer() {
		this.contextRunner.withUserConfiguration(CustomAwsClientConfig.class).run(context -> {
			SesClient sesClient = context.getBean(SesClient.class);

			Map attributeMap = (Map) ReflectionTestUtils.getField(ReflectionTestUtils.getField(
					ReflectionTestUtils.getField(sesClient, "clientConfiguration"), "attributes"), "attributes");
			assertThat(attributeMap.get(SdkClientOption.API_CALL_TIMEOUT)).isEqualTo(Duration.ofMillis(2000));
			assertThat(attributeMap.get(SdkClientOption.SYNC_HTTP_CLIENT)).isNotNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAwsClientConfig {

		@Bean
		CustomAwsClientConfig.SesAwsClientConfigurer<SesClientBuilder> snsClientBuilderAwsClientConfigurer() {
			return new CustomAwsClientConfig.SesAwsClientConfigurer<>();
		}

		static class SesAwsClientConfigurer<T extends AwsClientBuilder<?,?>> implements AwsClientConfigurer<SesClientBuilder> {
			@Override
			@Nullable
			public ClientOverrideConfiguration overrideConfiguration() {
				return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(2000)).build();
			}

			@Override
			@Nullable
			public <T extends SdkHttpClient> SdkHttpClient httpClient() {
				return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
			}
		}

	}

}
