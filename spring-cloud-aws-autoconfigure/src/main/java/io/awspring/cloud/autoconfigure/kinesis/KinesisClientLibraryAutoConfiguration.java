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
package io.awspring.cloud.autoconfigure.kinesis;

import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.metrics.CloudWatchExportAutoConfiguration;
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

@AutoConfiguration
@ConditionalOnClass({ KinesisAsyncClient.class, Scheduler.class })
@EnableConfigurationProperties({ KinesisClientLibraryProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class,
		KinesisAutoConfiguration.class, CloudWatchExportAutoConfiguration.class})
@ConditionalOnProperty(value = "spring.cloud.aws.kinesis.client.library.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisClientLibraryAutoConfiguration {
	z

	@ConditionalOnMissingBean
	@Bean
	public Scheduler scheduler(DynamoDbAsyncClient dynamoDbClient,
			CloudWatchAsyncClient cloudWatchClient, KinesisAsyncClient kinesisAsyncClient,
			KinesisClientLibraryProperties properties, ShardRecordProcessorFactory processorFactory) {
		return null;
	}
}
