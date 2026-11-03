/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.kinesis;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.kinesis.config.KclBeanNames;
import io.awspring.cloud.kinesis.config.KclMessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.KclContainerOptions;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import io.awspring.cloud.kinesis.stream.binder.KinesisMessageChannelBinder;
import io.awspring.cloud.kinesis.support.converter.EmbeddedHeadersKinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter;
import io.awspring.cloud.kinesis.support.converter.KinesisMessagingMessageConverter;
import io.awspring.cloud.kinesis.support.observation.KclListenerObservation;
import io.awspring.cloud.kinesis.support.observation.KinesisTemplateObservation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
class KinesisAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, KinesisAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void isDisabledWhenPropertySetToFalse() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(KinesisTemplate.class));
	}

	@Test
	void createsClientsTemplateAndFactory() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(KinesisAsyncClient.class);
			assertThat(context).hasSingleBean(DynamoDbAsyncClient.class);
			assertThat(context).hasSingleBean(CloudWatchAsyncClient.class);
			assertThat(context).hasSingleBean(KinesisTemplate.class);
			assertThat(context).hasSingleBean(KclMessageListenerContainerFactory.class);
			assertThat(context).hasBean(KclBeanNames.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME);
		});
	}

	@Test
	void registersBinderMappingsButNotBinderInMainContext() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MappingsProvider.class);
			assertThat(context).doesNotHaveBean(KinesisMessageChannelBinder.class);
			assertThat(context).doesNotHaveBean(KinesisAutoConfiguration.KinesisBinderConfiguration.class);
		});
	}

	@Test
	void bindsListenerProperties() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.listener.max-records:250",
				"spring.cloud.aws.kinesis.listener.retrieval-mode:ENHANCED_FAN_OUT",
				"spring.cloud.aws.kinesis.listener.checkpoint-mode:MANUAL",
				"spring.cloud.aws.kinesis.listener.halted-shard-release-delay:90s").run(context -> {
					KinesisProperties properties = context.getBean(KinesisProperties.class);
					assertThat(properties.getListener().getMaxRecords()).isEqualTo(250);
					assertThat(properties.getListener().getRetrievalMode()).isEqualTo(RetrievalMode.ENHANCED_FAN_OUT);
					assertThat(properties.getListener().getCheckpointMode()).isEqualTo(KclCheckpointMode.MANUAL);
					assertThat(properties.getListener().getHaltedShardReleaseDelay()).isEqualTo(Duration.ofSeconds(90));
				});
	}

	@Test
	void mapsHaltedShardReleaseDelayOntoContainerOptions() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.listener.halted-shard-release-delay:45s")
				.run(context -> {
					KclMessageListenerContainerFactory factory = context
							.getBean(KclMessageListenerContainerFactory.class);
					assertThat(containerOptions(factory).getHaltedShardReleaseDelay())
							.isEqualTo(Duration.ofSeconds(45));
				});
	}

	@Test
	void haltedShardReleaseDelayIsUnsetByDefault() {
		this.contextRunner.run(context -> {
			KclMessageListenerContainerFactory factory = context.getBean(KclMessageListenerContainerFactory.class);
			assertThat(containerOptions(factory).getHaltedShardReleaseDelay()).isNull();
		});
	}

	@Test
	void wiresApplicationEventPublisherIntoTheDefaultFactory() {
		this.contextRunner.run(context -> {
			KclMessageListenerContainerFactory factory = context.getBean(KclMessageListenerContainerFactory.class);
			assertThat(ReflectionTestUtils.getField(factory, "applicationEventPublisher")).isNotNull();
		});
	}

	private static KclContainerOptions containerOptions(KclMessageListenerContainerFactory factory) {
		@SuppressWarnings("unchecked")
		Consumer<KclContainerOptions.Builder> optionsConsumer = (Consumer<KclContainerOptions.Builder>) ReflectionTestUtils
				.getField(factory, "optionsConsumer");
		KclContainerOptions.Builder builder = KclContainerOptions.builder();
		optionsConsumer.accept(builder);
		return builder.build();
	}

	@Test
	void embeddedHeadersConverterIsNotRegisteredByDefault() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(EmbeddedHeadersKinesisMessageConverter.class);
			assertThat(context).doesNotHaveBean(KinesisMessageConverter.class);
		});
	}

	@Test
	void registersEmbeddedHeadersConverterWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.embedded-headers:true").run(context -> {
			assertThat(context).hasSingleBean(EmbeddedHeadersKinesisMessageConverter.class);
			assertThat(context).hasSingleBean(KinesisMessageConverter.class);
			assertThat(context.getBean(KinesisMessageConverter.class))
					.isInstanceOf(EmbeddedHeadersKinesisMessageConverter.class);
		});
	}

	@Test
	void appliesEmbeddedHeadersConverterToTemplateAndFactory() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.embedded-headers:true").run(context -> {
			KinesisMessageConverter converter = context.getBean(KinesisMessageConverter.class);
			assertThat(context.getBean(KinesisTemplate.class)).extracting("messageConverter").isSameAs(converter);
			KclMessageListenerContainerFactory factory = context.getBean(KclMessageListenerContainerFactory.class);
			assertThat(containerOptions(factory).getMessageConverter()).isSameAs(converter);
		});
	}

	@Test
	void userSuppliedConverterWinsOverEmbeddedHeaders() {
		KinesisMessageConverter custom = new KinesisMessagingMessageConverter();
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.embedded-headers:true")
				.withBean(KinesisMessageConverter.class, () -> custom).run(context -> {
					assertThat(context).doesNotHaveBean(EmbeddedHeadersKinesisMessageConverter.class);
					assertThat(context.getBean(KinesisMessageConverter.class)).isSameAs(custom);
				});
	}

	@Test
	void observationIsDisabledByDefault() {
		this.contextRunner.withBean(ObservationRegistry.class, ObservationRegistry::create)
				.run(context -> assertThat(context.getBean(KinesisTemplate.class)).extracting("observationRegistry")
						.isSameAs(ObservationRegistry.NOOP));
	}

	@Test
	void appliesObservationRegistryAndConventionWhenEnabled() {
		ObservationRegistry registry = ObservationRegistry.create();
		KinesisTemplateObservation.Convention convention = new KinesisTemplateObservation.DefaultConvention();
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.observation-enabled:true")
				.withBean(ObservationRegistry.class, () -> registry)
				.withBean(KinesisTemplateObservation.Convention.class, () -> convention).run(context -> {
					KinesisTemplate template = context.getBean(KinesisTemplate.class);
					assertThat(template).extracting("observationRegistry").isSameAs(registry);
					assertThat(template).extracting("observationConvention").isSameAs(convention);
				});
	}

	@Test
	void listenerObservationIsDisabledByDefault() {
		this.contextRunner.withBean(ObservationRegistry.class, ObservationRegistry::create).run(context -> {
			KclMessageListenerContainerFactory factory = context.getBean(KclMessageListenerContainerFactory.class);
			assertThat(containerOptions(factory).getObservationRegistry()).isSameAs(ObservationRegistry.NOOP);
			assertThat(containerOptions(factory).getObservationConvention()).isNull();
		});
	}

	@Test
	void appliesListenerObservationRegistryAndConventionToFactoryWhenEnabled() {
		ObservationRegistry registry = ObservationRegistry.create();
		KclListenerObservation.Convention convention = new KclListenerObservation.DefaultConvention();
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.observation-enabled:true")
				.withBean(ObservationRegistry.class, () -> registry)
				.withBean(KclListenerObservation.Convention.class, () -> convention).run(context -> {
					KclMessageListenerContainerFactory factory = context
							.getBean(KclMessageListenerContainerFactory.class);
					assertThat(containerOptions(factory).getObservationRegistry()).isSameAs(registry);
					assertThat(containerOptions(factory).getObservationConvention()).isSameAs(convention);
				});
	}

}
