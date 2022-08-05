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
package io.awspring.cloud.autoconfigure.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

/**
 * Tests for class {@link SqsAutoConfiguration}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
class SqsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SqsAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void sqsAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(SqsAsyncClient.class));
	}

	@Test
	void sqsAutoConfigurationIsEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SqsAsyncClient.class);
			assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
			assertThat(context).hasBean("defaultSqsListenerContainerFactory");
			assertThat(context).hasBean("sqsAsyncClient");
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SqsAsyncClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("https://sqs.eu-west-1.amazonaws.com"));
		});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.endpoint:http://localhost:8090").run(context -> {
			assertThat(context).hasSingleBean(SqsAsyncClient.class);
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SqsAsyncClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(client.isEndpointOverridden()).isTrue();
		});
	}

	@Test
	void customSqsClientConfigurer() {
		this.contextRunner.withUserConfiguration(CustomAwsAsyncClientConfig.class).run(context -> {
			SqsAsyncClient sqsAsyncClient = context.getBean(SqsAsyncClient.class);
			Map<?, ?> attributeMap = getProperties(sqsAsyncClient);
			assertThat(attributeMap.get(SdkClientOption.API_CALL_TIMEOUT)).isEqualTo(Duration.ofMillis(1999));
			assertThat(attributeMap.get(SdkClientOption.ASYNC_HTTP_CLIENT)).isNotNull();
		});
	}

	private Map<?, ?> getProperties(SqsAsyncClient sqsAsyncClient) {
		Object clientConfiguration = Objects.requireNonNull(
				ReflectionTestUtils.getField(sqsAsyncClient, "clientConfiguration"),
				() -> "clientConfiguration field not found in " + sqsAsyncClient.getClass().getSimpleName());
		Object attributes = Objects.requireNonNull(ReflectionTestUtils.getField(clientConfiguration, "attributes"),
				() -> "attributes field not found in " + clientConfiguration.getClass().getSimpleName());
		return (Map<?, ?>) Objects.requireNonNull(ReflectionTestUtils.getField(attributes, "attributes"),
				() -> "attributes field not found in " + clientConfiguration.getClass().getSimpleName());
	}

	@Test
	void configuresFactoryComponents() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(CustomComponentsConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);
					assertThat(ReflectionTestUtils.getField(factory, "containerOptions")).isNotNull()
							.extracting("maxInflightMessagesPerQueue").isEqualTo(19);
					assertThat(ReflectionTestUtils.getField(factory, "errorHandler")).isNotNull();
					assertThat(ReflectionTestUtils.getField(factory, "messageInterceptors")).asList().isNotEmpty();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomComponentsConfiguration {

		@Bean
		AsyncErrorHandler<Object> asyncErrorHandler() {
			return (t, msg) -> CompletableFuture.completedFuture(null);
		}

		@Bean
		AsyncMessageInterceptor<?> asyncMessageInterceptor() {
			return new AsyncMessageInterceptor<Object>() {
				@Override
				public CompletableFuture<Message<Object>> intercept(Message<Object> message) {
					return CompletableFuture.completedFuture(message);
				}
			};
		}

		@Bean
		ContainerOptions containerOptions() {
			return ContainerOptions.create().maxInflightMessagesPerQueue(19);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAwsAsyncClientConfig {

		@Bean
		AwsClientCustomizer<SqsAsyncClientBuilder> sqsClientBuilderAwsClientConfigurer() {
			return new AwsClientCustomizer<SqsAsyncClientBuilder>() {
				@Override
				@Nullable
				public ClientOverrideConfiguration overrideConfiguration() {
					return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(1999)).build();
				}

				@Override
				@Nullable
				public SdkAsyncHttpClient asyncHttpClient() {
					return NettyNioAsyncHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
				}
			};
		}
	}

}
