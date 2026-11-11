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
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedAsyncClient;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.MessageListenerContainerFactory;
import io.awspring.cloud.sqs.config.SqsBeanNames;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.ContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.DefaultListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import io.awspring.cloud.sqs.support.observation.SqsTemplateObservation;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link SqsAutoConfiguration}.
 *
 * @author Tomaz Fernandes
 * @author Wei Jiang
 * @author Sharang Gupta
 */
class SqsAutoConfigurationTest {

	private static final String CUSTOM_MESSAGE_CONVERTER_BEAN_NAME = "customMessageConverter";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withClassLoader(new FilteredClassLoader(AmazonSQSExtendedAsyncClient.class))
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
	void sqsAutoConfigurationIsDisabledWhenSqsModuleIsNotInClassPath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SqsBootstrapConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(SqsAsyncClient.class));
	}

	@Test
	void sqsAutoConfigurationIsEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SqsAsyncClient.class);
			assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
			assertThat(context).hasBean(EndpointRegistrar.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME);
			assertThat(context).hasBean("sqsAsyncClient");
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SqsAsyncClient.class));
			assertThat(client.getEndpoint()).isEqualTo(URI.create("https://sqs.eu-west-1.amazonaws.com"));
		});
	}

	@Test
	void configuresSqsTemplate() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(SqsTemplate.class));
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
	void withCustomQueueNotFoundStrategy() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.queue-not-found-strategy=fail").run(context -> {
			assertThat(context).hasSingleBean(SqsProperties.class);
			SqsProperties sqsProperties = context.getBean(SqsProperties.class);
			assertThat(context).hasSingleBean(SqsAsyncClient.class);
			assertThat(context).hasSingleBean(SqsTemplate.class);
			assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
			assertThat(sqsProperties.getQueueNotFoundStrategy()).isEqualTo(QueueNotFoundStrategy.FAIL);
		});
	}

	@Test
	void withObservationEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.observation-enabled=true")
				.withUserConfiguration(TestObservationRegistryConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsProperties.class);
					SqsProperties sqsProperties = context.getBean(SqsProperties.class);
					assertThat(context).hasSingleBean(SqsAsyncClient.class);
					assertThat(context).hasSingleBean(SqsTemplate.class);
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					assertThat(context).hasSingleBean(TestObservationRegistry.class);
					assertThat(sqsProperties.isObservationEnabled()).isTrue();

					// Verify SqsTemplate has the observation registry configured
					SqsTemplate sqsTemplate = context.getBean(SqsTemplate.class);
					assertThat(sqsTemplate).extracting("observationRegistry")
							.isEqualTo(context.getBean(ObservationRegistry.class));

					// Verify SqsMessageListenerContainerFactory has the observation registry configured
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);
					assertThat(factory).extracting("containerOptionsBuilder")
							.asInstanceOf(type(ContainerOptionsBuilder.class))
							.extracting(ContainerOptionsBuilder::build).extracting("observationRegistry")
							.isEqualTo(context.getBean(ObservationRegistry.class));
				});
	}

	@Test
	void withCustomObservationConventions() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.observation-enabled=true")
				.withUserConfiguration(TestObservationRegistryConfiguration.class,
						CustomObservationConventionConfiguration.class)
				.run(context -> {
					assertThat(context).hasSingleBean(SqsProperties.class);
					assertThat(context).hasSingleBean(SqsTemplate.class);
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					assertThat(context).hasSingleBean(TestObservationRegistry.class);
					assertThat(context).hasSingleBean(SqsTemplateObservation.Convention.class);
					assertThat(context).hasSingleBean(SqsListenerObservation.Convention.class);

					// Verify SqsTemplate has the custom observation convention configured
					SqsTemplate sqsTemplate = context.getBean(SqsTemplate.class);
					assertThat(sqsTemplate).extracting("customObservationConvention")
							.isEqualTo(context.getBean(SqsTemplateObservation.Convention.class));

					// Verify SqsMessageListenerContainerFactory has the custom observation convention configured
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);
					assertThat(factory).extracting("containerOptionsBuilder")
							.asInstanceOf(type(ContainerOptionsBuilder.class))
							.extracting(ContainerOptionsBuilder::build).extracting("observationConvention")
							.isEqualTo(context.getBean(SqsListenerObservation.Convention.class));
				});
	}

	@Test
	void withObservationDisabled() {
		this.contextRunner.withUserConfiguration(TestObservationRegistryConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(SqsProperties.class);
			SqsProperties sqsProperties = context.getBean(SqsProperties.class);
			assertThat(sqsProperties.isObservationEnabled()).isFalse();

			// Verify SqsTemplate has the default NOOP observation registry
			SqsTemplate sqsTemplate = context.getBean(SqsTemplate.class);
			assertThat(sqsTemplate).extracting("observationRegistry")
					.isNotEqualTo(context.getBean(ObservationRegistry.class))
					.asInstanceOf(type(ObservationRegistry.class)).extracting(ObservationRegistry::isNoop)
					.isEqualTo(true);

			// Verify SqsMessageListenerContainerFactory has the default NOOP observation registry
			SqsMessageListenerContainerFactory<?> factory = context.getBean(SqsMessageListenerContainerFactory.class);
			assertThat(factory).extracting("containerOptionsBuilder").asInstanceOf(type(ContainerOptionsBuilder.class))
					.extracting(ContainerOptionsBuilder::build).extracting("observationRegistry")
					.isNotEqualTo(context.getBean(ObservationRegistry.class))
					.asInstanceOf(type(ObservationRegistry.class)).extracting(ObservationRegistry::isNoop)
					.isEqualTo(true);
		});
	}

	@Test
	void configuresFactoryComponentsAndOptionsWithDefaults() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
			var factory = context.getBean(SqsMessageListenerContainerFactory.class);
			assertThat(factory).hasFieldOrProperty("errorHandler").extracting("asyncMessageInterceptors").asList()
					.isEmpty();
			assertThat(factory).extracting("acknowledgementResultCallback").isNull();
			assertThat(factory).extracting("asyncAcknowledgementResultCallback").isNull();
			assertThat(factory).extracting("containerOptionsBuilder").asInstanceOf(type(ContainerOptionsBuilder.class))
					.extracting(ContainerOptionsBuilder::build)
					.isInstanceOfSatisfying(ContainerOptions.class, options -> {
						assertThat(options.getMaxConcurrentMessages()).isEqualTo(10);
						assertThat(options.getMaxMessagesPerPoll()).isEqualTo(10);
						assertThat(options.getPollTimeout()).isEqualTo(Duration.ofSeconds(10));
						assertThat(options.getMaxDelayBetweenPolls()).isEqualTo(Duration.ofSeconds(10));
					}).extracting("messageConverter").asInstanceOf(type(SqsMessagingMessageConverter.class))
					.extracting("payloadMessageConverter").asInstanceOf(type(CompositeMessageConverter.class))
					.extracting(CompositeMessageConverter::getConverters)
					.isInstanceOfSatisfying(List.class, converters -> assertThat(converters.get(2)));
		});
	}
	// @formatter:on

	@Test
	void configuresFactoryWithBlockingAcknowledgementCallback() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(BlockingAcknowledgementCallbackConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					assertThat(context).hasSingleBean(AcknowledgementResultCallback.class);

					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);

					assertThat(factory).extracting("acknowledgementResultCallback")
							.isEqualTo(context.getBean(AcknowledgementResultCallback.class));
				});
	}

	@Test
	void configuresFactoryWithAsyncAcknowledgementCallback() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(AsyncAcknowledgementCallbackConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					assertThat(context).hasSingleBean(AsyncAcknowledgementResultCallback.class);

					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);

					assertThat(factory).extracting("asyncAcknowledgementResultCallback")
							.isEqualTo(context.getBean(AsyncAcknowledgementResultCallback.class));
				});
	}

	@Test
	void configuresMessageConverter() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(MessageConverterConfiguration.class).run(context -> {
					SqsTemplate sqsTemplate = context.getBean("sqsTemplate", SqsTemplate.class);
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean("defaultSqsListenerContainerFactory", SqsMessageListenerContainerFactory.class);
					SqsMessagingMessageConverter converter = context.getBean(CUSTOM_MESSAGE_CONVERTER_BEAN_NAME,
							SqsMessagingMessageConverter.class);
					assertThat(sqsTemplate).extracting("messageConverter").isEqualTo(converter);
					assertThat(factory).extracting("containerOptionsBuilder").extracting("messageConverter")
							.isEqualTo(converter);
				});
	}

	@Test
	void usesCustomJsonMapperWhenProvided() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(CustomJsonMapperConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(JsonMapper.class);
					JsonMapper customJsonMapper = context.getBean(JsonMapper.class);

					MessagingMessageConverter<Message> messageConverter = context
							.getBean(MessagingMessageConverter.class);
					assertThat(messageConverter).isInstanceOf(SqsMessagingMessageConverter.class);

					// Verify the custom JsonMapper is used in the converter chain
					assertThat(messageConverter).extracting("payloadMessageConverter")
							.asInstanceOf(type(CompositeMessageConverter.class))
							.extracting(CompositeMessageConverter::getConverters).asList()
							.filteredOn(converter -> converter instanceof JacksonJsonMessageConverter).hasSize(1)
							.first().extracting("mapper").isSameAs(customJsonMapper);
				});
	}

	@Test
	void usesDefaultJsonMapperWhenNoneProvided() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true").run(context -> {
			assertThat(context).doesNotHaveBean(JsonMapper.class);

			MessagingMessageConverter<Message> messageConverter = context.getBean(MessagingMessageConverter.class);
			assertThat(messageConverter).isInstanceOf(SqsMessagingMessageConverter.class);

			// Verify a JacksonJsonMessageConverter is still present with a default mapper
			assertThat(messageConverter).extracting("payloadMessageConverter")
					.asInstanceOf(type(CompositeMessageConverter.class))
					.extracting(CompositeMessageConverter::getConverters).asList()
					.filteredOn(converter -> converter instanceof JacksonJsonMessageConverter).hasSize(1).first()
					.extracting("mapper").isNotNull();
		});
	}

	@Test
	void createsExtendedClientWhenClassOnClasspathAndBucketConfigured() {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
						"spring.cloud.aws.sqs.extended.bucket=my-bucket")
				.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
						CredentialsProviderAutoConfiguration.class, SqsAutoConfiguration.class,
						AwsAutoConfiguration.class))
				.withUserConfiguration(S3AsyncClientConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsAsyncClient.class);
					assertThat(context.getBean(SqsAsyncClient.class)).isInstanceOf(AmazonSQSExtendedAsyncClient.class);
				});
	}

	@Test
	void createsNormalClientWhenExtendedClientOnClasspathButNoBucket() {
		new ApplicationContextRunner().withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
				.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
						CredentialsProviderAutoConfiguration.class, SqsAutoConfiguration.class,
						AwsAutoConfiguration.class))
				.withUserConfiguration(S3AsyncClientConfiguration.class).run(context -> {
					assertThat(context).hasFailed();
				});
	}

	@Test
	void createsNormalClientWhenExtendedClientNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(AmazonSQSExtendedAsyncClient.class)).run(context -> {
			assertThat(context).hasSingleBean(SqsAsyncClient.class);
			assertThat(context.getBean(SqsAsyncClient.class)).isNotInstanceOf(AmazonSQSExtendedAsyncClient.class);
		});
	}

	@Test
	void startsContainersInLifecyclePhaseByDefault() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MessageListenerContainerRegistry.class);
			assertThat(context.getBean(SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME)).isInstanceOfSatisfying(
					DefaultListenerContainerRegistry.class,
					registry -> assertThat(registry.isStartAfterContextRefresh()).isFalse());
		});
	}

	@Test
	void startsContainersAfterContextRefreshWhenConfigured() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.listener.start-after-context-refresh=true")
				.run(context -> {
					assertThat(context).hasSingleBean(MessageListenerContainerRegistry.class);
					assertThat(context.getBean(SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME)).isInstanceOfSatisfying(
							DefaultListenerContainerRegistry.class,
							registry -> assertThat(registry.isStartAfterContextRefresh()).isTrue());
				});
	}

	@Test
	void startAfterContextRefreshBacksOffWhenRegistryIsProvided() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.listener.start-after-context-refresh=true")
				.withUserConfiguration(CustomRegistryConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(MessageListenerContainerRegistry.class);
					assertThat(context.getBean(SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME))
							.isSameAs(CustomRegistryConfiguration.REGISTRY);
				});
	}

	@Test
	void startsListenerContainerAfterContextRefreshWithLazyInitialization() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.listener.start-after-context-refresh=true")
				.withBean(LazyInitializationBeanFactoryPostProcessor.class)
				.withUserConfiguration(StubListenerConfiguration.class).run(context -> {
					assertThat(context).hasNotFailed();
					MessageListenerContainerRegistry registry = context.getBean(MessageListenerContainerRegistry.class);
					assertThat(registry.getListenerContainers()).containsExactly(StubListenerConfiguration.CONTAINER);
					then(StubListenerConfiguration.CONTAINER).should().start();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRegistryConfiguration {

		static final DefaultListenerContainerRegistry REGISTRY = new DefaultListenerContainerRegistry();

		@Bean(name = SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME)
		DefaultListenerContainerRegistry customRegistry() {
			return REGISTRY;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StubListenerConfiguration {

		static final String STUB_FACTORY_BEAN_NAME = "stubContainerFactory";

		@SuppressWarnings("unchecked")
		static final MessageListenerContainer<Object> CONTAINER = mock(MessageListenerContainer.class);

		@Bean(name = STUB_FACTORY_BEAN_NAME)
		MessageListenerContainerFactory<MessageListenerContainer<Object>> stubContainerFactory() {
			given(CONTAINER.getId()).willReturn("stub-container");
			given(CONTAINER.isAutoStartup()).willReturn(true);
			return new MessageListenerContainerFactory<>() {

				@Override
				public MessageListenerContainer<Object> createContainer(String... logicalEndpointNames) {
					return CONTAINER;
				}

				@Override
				public MessageListenerContainer<Object> createContainer(Endpoint endpoint) {
					return CONTAINER;
				}

			};
		}

		@Lazy(false)
		@Bean
		StubListener stubListener() {
			return new StubListener();
		}

	}

	static class StubListener {

		@SqsListener(queueNames = "stub-queue", factory = StubListenerConfiguration.STUB_FACTORY_BEAN_NAME)
		void listen(String message) {
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class S3AsyncClientConfiguration {

		@Bean
		S3AsyncClient s3AsyncClient() {
			return Mockito.mock(S3AsyncClient.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomComponentsConfiguration {

		@Bean
		AsyncErrorHandler<Object> asyncErrorHandler() {
			return new AsyncErrorHandler<>() {
			};
		}

		@Bean
		AsyncMessageInterceptor<?> asyncMessageInterceptor() {
			return new AsyncMessageInterceptor<>() {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestObservationRegistryConfiguration {

		@Bean
		TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomObservationConventionConfiguration {

		@Bean
		SqsTemplateObservation.Convention customSqsTemplateObservationConvention() {
			return new SqsTemplateObservation.DefaultConvention() {
				@Override
				protected KeyValues getCustomHighCardinalityKeyValues(SqsTemplateObservation.Context context) {
					return KeyValues.of("payment.id", "test-payment-id");
				}
			};
		}

		@Bean
		SqsListenerObservation.Convention customSqsListenerObservationConvention() {
			return new SqsListenerObservation.DefaultConvention() {
				@Override
				protected KeyValues getCustomHighCardinalityKeyValues(SqsListenerObservation.Context context) {
					return KeyValues.of("order.id", "test-order-id");
				}
			};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MessageConverterConfiguration {

		@Bean(name = CUSTOM_MESSAGE_CONVERTER_BEAN_NAME)
		MessagingMessageConverter<Message> messageConverter() {
			return new SqsMessagingMessageConverter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJsonMapperConfiguration {

		@Bean
		JsonMapper jsonMapper() {
			return JsonMapper.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BlockingAcknowledgementCallbackConfiguration {

		@Bean
		AcknowledgementResultCallback<Object> acknowledgementResultCallback() {
			return new AcknowledgementResultCallback<>() {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AsyncAcknowledgementCallbackConfiguration {

		@Bean
		AsyncAcknowledgementResultCallback<Object> asyncAcknowledgementResultCallback() {
			return new AsyncAcknowledgementResultCallback<>() {
			};
		}

	}

}
