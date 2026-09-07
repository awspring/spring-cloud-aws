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

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.kinesis.config.KclBootstrapConfiguration;
import io.awspring.cloud.kinesis.config.KclMessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.KclContainerOptions;
import io.awspring.cloud.kinesis.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.kinesis.operations.KinesisOperations;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
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
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer
				.configure(KinesisAsyncClient.builder(), this.properties, connectionDetails.getIfAvailable()).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbAsyncClient kinesisDynamoDbAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer
				.configure(DynamoDbAsyncClient.builder(), this.properties, connectionDetails.getIfAvailable()).build();
	}

	@ConditionalOnMissingBean
	@Bean
	public CloudWatchAsyncClient kinesisCloudWatchAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer
				.configure(CloudWatchAsyncClient.builder(), this.properties, connectionDetails.getIfAvailable())
				.build();
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

}
