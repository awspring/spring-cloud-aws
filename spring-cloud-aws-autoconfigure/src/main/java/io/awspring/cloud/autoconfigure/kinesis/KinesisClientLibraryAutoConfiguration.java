package io.awspring.cloud.autoconfigure.kinesis;


import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import java.util.UUID;

@AutoConfiguration
@ConditionalOnClass({ KinesisAsyncClient.class, Scheduler.class })
@EnableConfigurationProperties({ KinesisClientLibraryProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class, KinesisAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.aws.kinesis.client.library.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisClientLibraryAutoConfiguration {



	@ConditionalOnMissingBean
	@Bean
	public Scheduler scheduler(ObjectProvider<DynamoDbAsyncClient> dynamoDbClient, ObjectProvider<CloudWatchAsyncClient> cloudWatchClient,
							   KinesisAsyncClient kinesisAsyncClient, KinesisClientLibraryProperties properties,
							   ShardRecordProcessorFactory processorFactory) {
		return null;
	}
}
