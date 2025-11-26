/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.stream.binder.config;

import io.awspring.cloud.autoconfigure.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.dynamodb.DynamoDbProperties;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchProperties;
import io.awspring.cloud.dynamodb.DynamoDbLockRegistry;
import io.awspring.cloud.dynamodb.DynamoDbLockRepository;
import io.awspring.cloud.dynamodb.DynamoDbMetadataStore;
import io.awspring.cloud.kinesis.stream.binder.KinesisBinderHealthIndicator;
import io.awspring.cloud.kinesis.stream.binder.KinesisMessageChannelBinder;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisExtendedBindingProperties;
import io.awspring.cloud.kinesis.stream.binder.provisioning.KinesisStreamProvisioner;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binding.Bindable;
import org.springframework.cloud.stream.config.ConsumerEndpointCustomizer;
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.support.locks.LockRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.kinesis.producer.KinesisProducerConfiguration;

/**
 * The auto-configuration for AWS components and Spring Cloud Stream Kinesis Binder.
 *
 * @author Peter Oates
 * @author Artem Bilan
 * @author Arnaud Lecollaire
 * @author Asiel Caballero
 *
 * @since 4.0
 */
@AutoConfiguration
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnMissingBean(Binder.class)
@EnableConfigurationProperties({ KinesisBinderConfigurationProperties.class, KinesisExtendedBindingProperties.class,
		KinesisProperties.class, DynamoDbProperties.class, DynamoDbStreamsProperties.class,
		CloudWatchProperties.class })
public class KinesisBinderConfiguration {

	private final KinesisBinderConfigurationProperties configurationProperties;

	private final AwsCredentialsProvider awsCredentialsProvider;

	private final AwsClientBuilderConfigurer awsClientBuilderConfigurer;

	private final Region region;

	private final boolean hasInputs;

	public KinesisBinderConfiguration(KinesisBinderConfigurationProperties configurationProperties,
			AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider regionProvider,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer, List<Bindable> bindables) {

		this.configurationProperties = configurationProperties;
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.awsClientBuilderConfigurer = awsClientBuilderConfigurer;
		this.region = regionProvider.getRegion();
		this.hasInputs = bindables.stream().map(Bindable::getInputs).flatMap(Set::stream).findFirst().isPresent();
	}

	@Bean
	@ConditionalOnMissingBean
	public KinesisAsyncClient amazonKinesis(KinesisProperties properties,
			ObjectProvider<AwsClientCustomizer<KinesisAsyncClientBuilder>> configurer) {

		return awsClientBuilderConfigurer
				.configureAsyncClient(KinesisAsyncClient.builder(), properties, null, configurer.stream(), null)
				.build();
	}

	@Bean
	public KinesisStreamProvisioner provisioningProvider(KinesisAsyncClient amazonKinesis) {
		return new KinesisStreamProvisioner(amazonKinesis, this.configurationProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public DynamoDbAsyncClient dynamoDB(DynamoDbProperties properties,
			ObjectProvider<AwsClientCustomizer<DynamoDbAsyncClientBuilder>> configurer) {

		if (this.hasInputs) {
			return awsClientBuilderConfigurer
					.configureAsyncClient(DynamoDbAsyncClient.builder(), properties, null, configurer.stream(), null)
					.build();
		}
		else {
			return null;
		}
	}

	@Bean
	@ConditionalOnMissingBean(LockRegistry.class)
	@ConditionalOnBean(DynamoDbAsyncClient.class)
	@ConditionalOnProperty(name = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled", havingValue = "false", matchIfMissing = true)
	public DynamoDbLockRepository dynamoDbLockRepository(@Autowired(required = false) DynamoDbAsyncClient dynamoDB) {
		if (dynamoDB != null) {
			KinesisBinderConfigurationProperties.Locks locks = this.configurationProperties.getLocks();
			DynamoDbLockRepository dynamoDbLockRepository = new DynamoDbLockRepository(dynamoDB, locks.getTable());
			dynamoDbLockRepository.setBillingMode(locks.getBillingMode());
			dynamoDbLockRepository.setReadCapacity(locks.getReadCapacity());
			dynamoDbLockRepository.setWriteCapacity(locks.getWriteCapacity());
			return dynamoDbLockRepository;
		}
		else {
			return null;
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(DynamoDbAsyncClient.class)
	@ConditionalOnProperty(name = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled", havingValue = "false", matchIfMissing = true)
	public LockRegistry<?> dynamoDBLockRegistry(
			@Autowired(required = false) DynamoDbLockRepository dynamoDbLockRepository) {

		if (dynamoDbLockRepository != null) {
			KinesisBinderConfigurationProperties.Locks locks = this.configurationProperties.getLocks();
			DynamoDbLockRegistry dynamoDbLockRegistry = new DynamoDbLockRegistry(dynamoDbLockRepository);
			dynamoDbLockRegistry.setIdleBetweenTries(locks.getRefreshPeriod());
			dynamoDbLockRegistry.setTimeToLive(locks.getLeaseDuration());
			return dynamoDbLockRegistry;
		}
		else {
			return null;
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(DynamoDbAsyncClient.class)
	@ConditionalOnProperty(name = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled", havingValue = "false", matchIfMissing = true)
	public ConcurrentMetadataStore kinesisCheckpointStore(@Autowired(required = false) DynamoDbAsyncClient dynamoDB) {
		if (dynamoDB != null) {
			KinesisBinderConfigurationProperties.Checkpoint checkpoint = this.configurationProperties.getCheckpoint();
			DynamoDbMetadataStore kinesisCheckpointStore = new DynamoDbMetadataStore(dynamoDB, checkpoint.getTable());
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
		else {
			return null;
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled")
	public CloudWatchAsyncClient cloudWatch(CloudWatchProperties properties,
			ObjectProvider<AwsClientCustomizer<CloudWatchAsyncClientBuilder>> configurer) {

		if (this.hasInputs) {
			return awsClientBuilderConfigurer.configureAsyncClient(CloudWatchAsyncClient.builder(), properties, null,
					Stream.of(configurer.getIfAvailable()), null).build();
		}
		else {
			return null;
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.cloud.stream.kinesis.binder.kpl-kcl-enabled")
	public KinesisProducerConfiguration kinesisProducerConfiguration() {
		KinesisProducerConfiguration kinesisProducerConfiguration = new KinesisProducerConfiguration();
		kinesisProducerConfiguration.setCredentialsProvider(this.awsCredentialsProvider);
		kinesisProducerConfiguration.setRegion(this.region.id());
		return kinesisProducerConfiguration;
	}

	@Bean
	@ConditionalOnMissingBean
	public DynamoDbStreamsClient dynamoDBStreams(DynamoDbStreamsProperties properties,
			ObjectProvider<AwsClientCustomizer<DynamoDbStreamsClientBuilder>> configurer) {

		if (this.hasInputs) {
			return awsClientBuilderConfigurer
					.configureAsyncClient(DynamoDbStreamsClient.builder(), properties, null, configurer.stream(), null)
					.build();
		}
		else {
			return null;
		}
	}

	@Bean
	public KinesisMessageChannelBinder kinesisMessageChannelBinder(KinesisStreamProvisioner provisioningProvider,
			KinesisAsyncClient amazonKinesis, KinesisExtendedBindingProperties kinesisExtendedBindingProperties,
			@Autowired(required = false) ConcurrentMetadataStore kinesisCheckpointStore,
			@Autowired(required = false) LockRegistry<?> lockRegistry,
			@Autowired(required = false) DynamoDbAsyncClient dynamoDBClient,
			@Autowired(required = false) DynamoDbStreamsClient dynamoDBStreams,
			@Autowired(required = false) CloudWatchAsyncClient cloudWatchClient,
			@Autowired(required = false) KinesisProducerConfiguration kinesisProducerConfiguration,
			@Autowired(required = false) ProducerMessageHandlerCustomizer<? extends AbstractMessageProducingHandler> producerMessageHandlerCustomizer,
			@Autowired(required = false) ConsumerEndpointCustomizer<? extends MessageProducerSupport> consumerEndpointCustomizer,
			@Autowired ObservationRegistry observationRegistry) {

		KinesisMessageChannelBinder kinesisMessageChannelBinder = new KinesisMessageChannelBinder(
				this.configurationProperties, provisioningProvider, amazonKinesis, dynamoDBClient, dynamoDBStreams,
				cloudWatchClient);
		kinesisMessageChannelBinder.setCheckpointStore(kinesisCheckpointStore);
		kinesisMessageChannelBinder.setLockRegistry(lockRegistry);
		kinesisMessageChannelBinder.setExtendedBindingProperties(kinesisExtendedBindingProperties);
		kinesisMessageChannelBinder.setKinesisProducerConfiguration(kinesisProducerConfiguration);
		kinesisMessageChannelBinder.setProducerMessageHandlerCustomizer(producerMessageHandlerCustomizer);
		kinesisMessageChannelBinder.setConsumerEndpointCustomizer(consumerEndpointCustomizer);
		if (this.configurationProperties.isEnableObservation()) {
			kinesisMessageChannelBinder.setObservationRegistry(observationRegistry);
		}
		return kinesisMessageChannelBinder;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HealthIndicator.class)
	@ConditionalOnEnabledHealthIndicator("binders")
	protected static class KinesisBinderHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "kinesisBinderHealthIndicator")
		public KinesisBinderHealthIndicator kinesisBinderHealthIndicator(
				KinesisMessageChannelBinder kinesisMessageChannelBinder) {

			return new KinesisBinderHealthIndicator(kinesisMessageChannelBinder);
		}

	}

}
