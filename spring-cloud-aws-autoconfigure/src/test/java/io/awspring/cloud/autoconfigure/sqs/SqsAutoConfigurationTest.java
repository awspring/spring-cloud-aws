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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sqs.annotation.SqsListenerAnnotationBeanPostProcessor;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.ContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Tests for {@link SqsAutoConfiguration}.
 *
 * @author Tomaz Fernandes
 * @author Wei Jiang
 */
class SqsAutoConfigurationTest {

	private static final String CUSTOM_OBJECT_MAPPER_BEAN_NAME = "customObjectMapper";
	private static final String CUSTOM_MESSAGE_CONVERTER_BEAN_NAME = "customMessageConverter";

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
	void configuresFactoryComponentsAndOptions() {
		this.contextRunner
				.withPropertyValues("spring.cloud.aws.sqs.enabled:true",
						"spring.cloud.aws.sqs.listener.max-concurrent-messages:19",
						"spring.cloud.aws.sqs.listener.max-messages-per-poll:8",
						"spring.cloud.aws.sqs.listener.poll-timeout:6s",
						"spring.cloud.aws.sqs.listener.max-delay-between-polls:15s",
						"spring.cloud.aws.sqs.listener.auto-startup=false")
				.withUserConfiguration(CustomComponentsConfiguration.class, ObjectMapperConfiguration.class).run(context -> {
					assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean(SqsMessageListenerContainerFactory.class);
				assertThat(factory)
					.hasFieldOrProperty("errorHandler")
					.extracting("asyncMessageInterceptors").asList().isNotEmpty();
				assertThat(factory)
					.extracting("containerOptionsBuilder")
					.asInstanceOf(type(ContainerOptionsBuilder.class))
					.extracting(ContainerOptionsBuilder::build)
					.isInstanceOfSatisfying(ContainerOptions.class, options -> {
						assertThat(options.getMaxConcurrentMessages()).isEqualTo(19);
						assertThat(options.getMaxMessagesPerPoll()).isEqualTo(8);
						assertThat(options.getPollTimeout()).isEqualTo(Duration.ofSeconds(6));
						assertThat(options.getMaxDelayBetweenPolls()).isEqualTo(Duration.ofSeconds(15));
						assertThat(options.isAutoStartup()).isEqualTo(false);
					})
					.extracting("messageConverter")
					.asInstanceOf(type(SqsMessagingMessageConverter.class))
					.extracting("payloadMessageConverter")
					.asInstanceOf(type(CompositeMessageConverter.class))
					.extracting(CompositeMessageConverter::getConverters)
					.isInstanceOfSatisfying(List.class, converters ->
						assertThat(converters.get(2)).isInstanceOfSatisfying(
							MappingJackson2MessageConverter.class,
							jackson2MessageConverter ->
								assertThat(jackson2MessageConverter.getObjectMapper().getRegisteredModuleIds()).contains("jackson-datatype-jsr310")));
				});
	}

	@Test
	void configuresFactoryComponentsAndOptionsWithDefaults() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SqsMessageListenerContainerFactory.class);
			var factory = context.getBean(SqsMessageListenerContainerFactory.class);
			assertThat(factory).hasFieldOrProperty("errorHandler").extracting("asyncMessageInterceptors").asList()
					.isEmpty();
			assertThat(factory).extracting("containerOptionsBuilder").asInstanceOf(type(ContainerOptionsBuilder.class))
				.extracting(ContainerOptionsBuilder::build)
				.isInstanceOfSatisfying(ContainerOptions.class, options -> {
					assertThat(options.getMaxConcurrentMessages()).isEqualTo(10);
					assertThat(options.getMaxMessagesPerPoll()).isEqualTo(10);
					assertThat(options.getPollTimeout()).isEqualTo(Duration.ofSeconds(10));
					assertThat(options.getMaxDelayBetweenPolls()).isEqualTo(Duration.ofSeconds(10));
				})
				.extracting("messageConverter")
				.asInstanceOf(type(SqsMessagingMessageConverter.class))
				.extracting("payloadMessageConverter")
				.asInstanceOf(type(CompositeMessageConverter.class))
				.extracting(CompositeMessageConverter::getConverters)
				.isInstanceOfSatisfying(List.class, converters ->
					assertThat(converters.get(2)).isInstanceOfSatisfying(
						MappingJackson2MessageConverter.class,
						jackson2MessageConverter ->
							assertThat(jackson2MessageConverter.getObjectMapper().getRegisteredModuleIds()).isEmpty()));
		});
	}
	// @formatter:on

	@Test
	void configuresObjectMapper() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(ObjectMapperConfiguration.class).run(context -> {
					SqsListenerAnnotationBeanPostProcessor bpp = context
							.getBean(SqsListenerAnnotationBeanPostProcessor.class);
					ObjectMapper objectMapper = context.getBean(CUSTOM_OBJECT_MAPPER_BEAN_NAME, ObjectMapper.class);
					assertThat(bpp).extracting("endpointRegistrar").asInstanceOf(type(EndpointRegistrar.class))
							.extracting(EndpointRegistrar::getObjectMapper).isEqualTo(objectMapper);
				});
	}

	@Test
	void configuresMessageConverter() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sqs.enabled:true")
				.withUserConfiguration(ObjectMapperConfiguration.class, MessageConverterConfiguration.class)
				.run(context -> {
					SqsTemplate sqsTemplate = context.getBean("sqsTemplate", SqsTemplate.class);
					SqsMessageListenerContainerFactory<?> factory = context
							.getBean("defaultSqsListenerContainerFactory", SqsMessageListenerContainerFactory.class);
					ObjectMapper objectMapper = context.getBean(CUSTOM_OBJECT_MAPPER_BEAN_NAME, ObjectMapper.class);
					SqsMessagingMessageConverter converter = context.getBean(CUSTOM_MESSAGE_CONVERTER_BEAN_NAME,
							SqsMessagingMessageConverter.class);
					assertThat(converter.getPayloadMessageConverter()).extracting("converters").asList()
							.filteredOn(conv -> conv instanceof MappingJackson2MessageConverter).first()
							.extracting("objectMapper").isEqualTo(objectMapper);
					assertThat(sqsTemplate).extracting("messageConverter").isEqualTo(converter);
					assertThat(factory).extracting("containerOptionsBuilder").extracting("messageConverter")
							.isEqualTo(converter);
				});
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
	static class ObjectMapperConfiguration {

		@Bean(name = CUSTOM_OBJECT_MAPPER_BEAN_NAME)
		ObjectMapper objectMapper() {
			return new ObjectMapper().registerModule(new JavaTimeModule());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MessageConverterConfiguration {

		@Bean(name = CUSTOM_MESSAGE_CONVERTER_BEAN_NAME)
		MessagingMessageConverter<Message> messageConverter() {
			return new SqsMessagingMessageConverter();
		}

	}

}
