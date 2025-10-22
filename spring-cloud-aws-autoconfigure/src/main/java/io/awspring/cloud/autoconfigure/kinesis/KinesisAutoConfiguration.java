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

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import io.awspring.cloud.autoconfigure.AwsAsyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

@AutoConfiguration
@ConditionalOnClass({ KinesisAsyncClient.class })
@EnableConfigurationProperties({ KinesisProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(value = "spring.cloud.aws.kinesis.enabled", havingValue = "true", matchIfMissing = true)
public class KinesisAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public KinesisAsyncClient kinesisAsyncClient(KinesisProperties properties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<KinesisAsyncClientCustomizer> kinesisAsyncClientCustomizer,
			ObjectProvider<AwsAsyncClientCustomizer> awsSyncClientCustomizers) {
		return awsClientBuilderConfigurer
				.configureAsyncClient(KinesisAsyncClient.builder(), properties, connectionDetails.getIfAvailable(),
						kinesisAsyncClientCustomizer.orderedStream(), awsSyncClientCustomizers.orderedStream())
				.build();
	}

	@ConditionalOnClass(name = { "com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration",
			"com.amazonaws.services.kinesis.producer.KinesisProducer" })
	public static class KinesisProducerAutoConfiguration {
	}

	// In your configs
	ConfigsBuilder configsBuilder = new ConfigsBuilder(
		streamName,
		applicationName,
		KinesisClientUtil.createKinesisAsyncClient(kinesisAsyncClient), // <-- Use your bean
		dynamoDbClient,
		cloudWatchClient,
		workerId,
		recordProcessorFactory
	);

}
