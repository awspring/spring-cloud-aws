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

import io.awspring.cloud.autoconfigure.AwsAsyncClientCustomizer;
import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbLockRegistry;
import io.awspring.cloud.dynamodb.DynamoDbLockRepository;
import io.awspring.cloud.dynamodb.DynamoDbMetadataStore;
import io.awspring.cloud.kinesis.config.KclBootstrapConfiguration;
import io.awspring.cloud.kinesis.config.KclMessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.KclContainerOptions;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import io.awspring.cloud.kinesis.stream.binder.KinesisBinderHealthIndicator;
import io.awspring.cloud.kinesis.stream.binder.KinesisMessageChannelBinder;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisExtendedBindingProperties;
import io.awspring.cloud.kinesis.stream.binder.provisioning.KinesisStreamProvisioner;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.config.BindingHandlerAdvise.MappingsProvider;
import org.springframework.cloud.stream.config.ConsumerEndpointCustomizer;
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Auto-configuration for Kinesis: {@link KinesisTemplate}, {@code @KclListener} support and, when
 * {@code spring-cloud-aws-kinesis-stream-binder} is on the classpath, the Spring Cloud Stream Kinesis binder.
 *
 * @author Matej Nedic
 * @author Peter Oates
 * @author Artem Bilan
 * @author Arnaud Lecollaire
 * @author Asiel Caballero
 * @since 4.2.0
 */
@AutoConfiguration
@ConditionalOnClass({ KinesisAsyncClient.class, KclBootstrapConfiguration.class })
@EnableConfigurationProperties(KinesisProperties.class)
@Import(KclBootstrapConfiguration.class)
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.kinesis.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisAutoConfiguration {

	private final KinesisProperties properties;

	public KinesisAutoConfiguration(KinesisProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnMissingBean
	@Bean
	public KinesisAsyncClient kinesisAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<KinesisAsyncClientCustomizer> kinesisAsyncClientCustomizers,
			ObjectProvider<AwsAsyncClientCustomizer> awsAsyncClientCustomizers,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer
				.configureAsyncClient(KinesisAsyncClient.builder(), this.properties, connectionDetails.getIfAvailable(),
						kinesisAsyncClientCustomizers.orderedStream(), awsAsyncClientCustomizers.orderedStream())
				.build();
	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbAsyncClient kinesisDynamoDbAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<DynamoDbAsyncClientCustomizer> dynamoDbAsyncClientCustomizers,
			ObjectProvider<AwsAsyncClientCustomizer> awsAsyncClientCustomizers,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer.configureAsyncClient(DynamoDbAsyncClient.builder(), this.properties,
				connectionDetails.getIfAvailable(), dynamoDbAsyncClientCustomizers.orderedStream(),
				awsAsyncClientCustomizers.orderedStream()).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public CloudWatchAsyncClient kinesisCloudWatchAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<CloudwatchAsyncClientCustomizer> cloudwatchAsyncClientCustomizers,
			ObjectProvider<AwsAsyncClientCustomizer> awsAsyncClientCustomizers,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer.configureAsyncClient(CloudWatchAsyncClient.builder(), this.properties,
				connectionDetails.getIfAvailable(), cloudwatchAsyncClientCustomizers.orderedStream(),
				awsAsyncClientCustomizers.orderedStream()).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public KinesisTemplate kinesisTemplate(KinesisAsyncClient kinesisAsyncClient,
			ObjectProvider<JsonMapper> jsonMapperProvider) {
		return new KinesisTemplate(kinesisAsyncClient, jsonMapperProvider.getIfAvailable(JsonMapper::new));
	}

	@ConditionalOnMissingBean
	@Bean
	public KclMessageListenerContainerFactory defaultKclListenerContainerFactory(KinesisAsyncClient kinesisAsyncClient,
			DynamoDbAsyncClient dynamoDbAsyncClient, CloudWatchAsyncClient cloudWatchAsyncClient,
			ObjectProvider<ErrorHandler> errorHandler, ObjectProvider<KinesisOperations> kinesisOperations) {
		KclMessageListenerContainerFactory factory = new KclMessageListenerContainerFactory(kinesisAsyncClient,
				dynamoDbAsyncClient, cloudWatchAsyncClient);
		factory.configure(this::configureContainerOptions);
		errorHandler.ifUnique(factory::setErrorHandler);
		kinesisOperations.ifUnique(factory::setKinesisOperations);
		return factory;
	}

	private void configureContainerOptions(KclContainerOptions.Builder options) {
		PropertyMapper mapper = PropertyMapper.get();
		KinesisProperties.Listener listener = this.properties.getListener();
		mapper.from(listener.getMaxRecords()).to(options::maxRecords);
		mapper.from(listener.getIdleTimeBetweenReads())
				.to(duration -> options.idleTimeBetweenReadsInMillis(duration.toMillis()));
		mapper.from(listener.getRetrievalMode()).to(options::retrievalMode);
		mapper.from(listener.getCheckpointMode()).to(options::checkpointMode);
		mapper.from(listener.getInitialPosition()).to(options::initialPositionInStream);
		mapper.from(listener.getGracefulShutdownTimeout()).to(options::gracefulShutdownTimeout);
		mapper.from(listener.getCheckpointRecordCount()).to(options::checkpointRecordCount);
		mapper.from(listener.getCheckpointInterval()).to(options::checkpointInterval);
		mapper.from(listener.getMetricsLevel()).to(options::metricsLevel);
		mapper.from(listener.getMetricsNamespace()).to(options::metricsNamespace);
		mapper.from(listener.getAutoStartup()).to(options::autoStartup);
		mapper.from(listener.getPhase()).to(options::phase);
		mapper.from(listener.getInitialPositionTimestamp()).to(options::initialPositionTimestamp);
		mapper.from(listener.getBillingMode()).to(options::billingMode);
		mapper.from(listener.getContentType()).as(MimeTypeUtils::parseMimeType).to(options::payloadContentType);
	}

	/**
	 * Registers the default mappings for {@code spring.cloud.stream.kinesis.default} in the main application context
	 * when the Spring Cloud Stream Kinesis binder is on the classpath.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ KinesisMessageChannelBinder.class, MappingsProvider.class })
	static class KinesisBinderMappingsConfiguration {

		@Bean
		MappingsProvider kinesisExtendedPropertiesDefaultMappingsProvider() {
			return () -> Map.of(ConfigurationPropertyName.of("spring.cloud.stream.kinesis.bindings"),
					ConfigurationPropertyName.of("spring.cloud.stream.kinesis.default"));
		}

	}

	/**
	 * Spring Cloud Stream Kinesis binder configuration. Referenced from {@code META-INF/spring.binders} of
	 * {@code spring-cloud-aws-starter-stream-kinesis} and only applied in the binder child context, where AWS clients
	 * are resolved from the parent (main) application context.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(KinesisMessageChannelBinder.class)
	@Conditional(OnBinderChildContextCondition.class)
	@EnableConfigurationProperties({ KinesisBinderConfigurationProperties.class,
			KinesisExtendedBindingProperties.class })
	public static class KinesisBinderConfiguration {

		private static final String KPL_KCL_ENABLED = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled";

		private final KinesisBinderConfigurationProperties configurationProperties;

		KinesisBinderConfiguration(KinesisBinderConfigurationProperties configurationProperties) {
			this.configurationProperties = configurationProperties;
		}

		@Bean
		KinesisStreamProvisioner provisioningProvider(KinesisAsyncClient kinesisAsyncClient) {
			return new KinesisStreamProvisioner(kinesisAsyncClient, this.configurationProperties);
		}

		@Bean
		@ConditionalOnMissingBean
		DynamoDbStreamsClient dynamoDbStreamsClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
				KinesisProperties properties, ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers,
				ObjectProvider<AwsConnectionDetails> connectionDetails) {
			return awsClientBuilderConfigurer.configureSyncClient(DynamoDbStreamsClient.builder(), properties,
					connectionDetails.getIfAvailable(), null, awsSyncClientCustomizers.orderedStream()).build();
		}

		@Bean
		@ConditionalOnMissingBean(LockRegistry.class)
		@ConditionalOnProperty(name = KPL_KCL_ENABLED, havingValue = "false", matchIfMissing = true)
		DynamoDbLockRepository dynamoDbLockRepository(DynamoDbAsyncClient dynamoDbAsyncClient) {
			KinesisBinderConfigurationProperties.Locks locks = this.configurationProperties.getLocks();
			DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(dynamoDbAsyncClient,
					locks.getTable());
			dynamoDbLockRepository.setBillingMode(locks.getBillingMode());
			dynamoDbLockRepository.setReadCapacity(locks.getReadCapacity());
			dynamoDbLockRepository.setWriteCapacity(locks.getWriteCapacity());
			return dynamoDbLockRepository;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(DynamoDbLockRepository.class)
		@ConditionalOnProperty(name = KPL_KCL_ENABLED, havingValue = "false", matchIfMissing = true)
		LockRegistry<?> dynamoDbLockRegistry(DynamoDbLockRepository dynamoDbLockRepository) {
			KinesisBinderConfigurationProperties.Locks locks = this.configurationProperties.getLocks();
			DynamoDbLockRegistry dynamoDbLockRegistry = new DynamoDbLockRegistry(dynamoDbLockRepository);
			dynamoDbLockRegistry.setIdleBetweenTries(locks.getRefreshPeriod());
			dynamoDbLockRegistry.setTimeToLive(locks.getLeaseDuration());
			return dynamoDbLockRegistry;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(name = KPL_KCL_ENABLED, havingValue = "false", matchIfMissing = true)
		ConcurrentMetadataStore kinesisCheckpointStore(DynamoDbAsyncClient dynamoDbAsyncClient) {
			KinesisBinderConfigurationProperties.Checkpoint checkpoint = this.configurationProperties.getCheckpoint();
			DynamoDbMetadataStore kinesisCheckpointStore = new DynamoDbMetadataStore(dynamoDbAsyncClient,
					checkpoint.getTable());
			kinesisCheckpointStore.setBillingMode(checkpoint.getBillingMode());
			kinesisCheckpointStore.setReadCapacity(checkpoint.getReadCapacity());
			kinesisCheckpointStore.setWriteCapacity(checkpoint.getWriteCapacity());
			kinesisCheckpointStore.setCreateTableDelay(checkpoint.getCreateDelay());
			kinesisCheckpointStore.setCreateTableRetries(checkpoint.getCreateRetries());
			Integer timeToLive = checkpoint.getTimeToLive();
			if (timeToLive != null) {
				kinesisCheckpointStore.setTimeToLive(timeToLive);
			}
			return kinesisCheckpointStore;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(name = "software.amazon.kinesis.producer.KinesisProducerConfiguration")
		@ConditionalOnProperty(name = KPL_KCL_ENABLED)
		KinesisProducerConfiguration kinesisProducerConfiguration(AwsCredentialsProvider awsCredentialsProvider,
				AwsRegionProvider awsRegionProvider) {
			KinesisProducerConfiguration kinesisProducerConfiguration = new KinesisProducerConfiguration();
			kinesisProducerConfiguration.setCredentialsProvider(awsCredentialsProvider);
			kinesisProducerConfiguration.setRegion(awsRegionProvider.getRegion().id());
			return kinesisProducerConfiguration;
		}

		@Bean
		KinesisMessageChannelBinder kinesisMessageChannelBinder(KinesisStreamProvisioner provisioningProvider,
				KinesisAsyncClient kinesisAsyncClient, DynamoDbAsyncClient dynamoDbAsyncClient,
				CloudWatchAsyncClient cloudWatchAsyncClient, DynamoDbStreamsClient dynamoDbStreamsClient,
				KinesisExtendedBindingProperties kinesisExtendedBindingProperties,
				ObjectProvider<ConcurrentMetadataStore> checkpointStore, ObjectProvider<LockRegistry<?>> lockRegistry,
				ObjectProvider<KinesisProducerConfiguration> kinesisProducerConfiguration,
				ObjectProvider<ProducerMessageHandlerCustomizer<? extends AbstractMessageProducingHandler>> producerMessageHandlerCustomizer,
				ObjectProvider<ConsumerEndpointCustomizer<? extends MessageProducerSupport>> consumerEndpointCustomizer,
				ObjectProvider<ObservationRegistry> observationRegistry) {

			KinesisMessageChannelBinder binder = new KinesisMessageChannelBinder(this.configurationProperties,
					provisioningProvider, kinesisAsyncClient, dynamoDbAsyncClient, dynamoDbStreamsClient,
					cloudWatchAsyncClient);
			binder.setCheckpointStore(checkpointStore.getIfUnique());
			binder.setLockRegistry(lockRegistry.getIfUnique());
			binder.setExtendedBindingProperties(kinesisExtendedBindingProperties);
			binder.setKinesisProducerConfiguration(kinesisProducerConfiguration.getIfAvailable());
			binder.setProducerMessageHandlerCustomizer(producerMessageHandlerCustomizer.getIfUnique());
			binder.setConsumerEndpointCustomizer(consumerEndpointCustomizer.getIfUnique());
			if (this.configurationProperties.isEnableObservation()) {
				observationRegistry.ifAvailable(binder::setObservationRegistry);
			}
			return binder;
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(HealthIndicator.class)
		@ConditionalOnEnabledHealthIndicator("binders")
		static class KinesisBinderHealthIndicatorConfiguration {

			@Bean
			@ConditionalOnMissingBean(name = "kinesisBinderHealthIndicator")
			KinesisBinderHealthIndicator kinesisBinderHealthIndicator(
					KinesisMessageChannelBinder kinesisMessageChannelBinder) {
				return new KinesisBinderHealthIndicator(kinesisMessageChannelBinder);
			}

		}

	}

	/**
	 * Matches only in the Spring Cloud Stream binder child context, whose parent holds the {@link BinderFactory}.
	 * Prevents {@link KinesisBinderConfiguration} from being applied to the main context as a member class of
	 * {@link KinesisAutoConfiguration}.
	 */
	static class OnBinderChildContextCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			BeanFactory parent = beanFactory != null ? beanFactory.getParentBeanFactory() : null;
			if (parent instanceof ListableBeanFactory listableParent
					&& listableParent.getBeanNamesForType(BinderFactory.class, false, false).length > 0) {
				return ConditionOutcome.match("Parent context contains a Spring Cloud Stream BinderFactory");
			}
			return ConditionOutcome.noMatch("Not a Spring Cloud Stream binder child context");
		}

	}

}
