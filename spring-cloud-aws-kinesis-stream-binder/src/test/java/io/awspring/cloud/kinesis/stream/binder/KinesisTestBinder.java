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
package io.awspring.cloud.kinesis.stream.binder;

import io.awspring.cloud.kinesis.integration.KinesisMessageDrivenChannelAdapter;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisConsumerProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisProducerProperties;
import io.awspring.cloud.kinesis.stream.binder.provisioning.KinesisStreamProvisioner;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionTestSupport;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * An {@link AbstractTestBinder} implementation for the {@link KinesisMessageChannelBinder}.
 *
 * @author Artem Bilan
 * @author Arnaud Lecollaire
 *
 * @since 4.0
 */
public class KinesisTestBinder extends
		AbstractTestBinder<KinesisMessageChannelBinder, ExtendedConsumerProperties<KinesisConsumerProperties>, ExtendedProducerProperties<KinesisProducerProperties>> {

	private final KinesisAsyncClient amazonKinesis;

	private final GenericApplicationContext applicationContext;

	public KinesisTestBinder(KinesisAsyncClient amazonKinesis, DynamoDbAsyncClient dynamoDbClient,
			CloudWatchAsyncClient cloudWatchClient,
			KinesisBinderConfigurationProperties kinesisBinderConfigurationProperties) {

		this.applicationContext = new AnnotationConfigApplicationContext(Config.class);

		this.amazonKinesis = amazonKinesis;

		KinesisStreamProvisioner provisioningProvider = new KinesisStreamProvisioner(amazonKinesis,
				kinesisBinderConfigurationProperties);

		KinesisMessageChannelBinder binder = new TestKinesisMessageChannelBinder(amazonKinesis, dynamoDbClient,
				cloudWatchClient, kinesisBinderConfigurationProperties, provisioningProvider);

		binder.setApplicationContext(this.applicationContext);

		setBinder(binder);
	}

	public GenericApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void cleanup() {
		this.amazonKinesis.listStreams()
				.thenCompose(reply -> CompletableFuture.allOf(reply.streamNames().stream()
						.map(streamName -> this.amazonKinesis.deleteStream(request -> request.streamName(streamName))
								.thenCompose(result -> this.amazonKinesis.waiter()
										.waitUntilStreamNotExists(request -> request.streamName(streamName))))
						.toArray(CompletableFuture[]::new)))
				.join();
	}

	/**
	 * Test configuration.
	 */
	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		public PartitionTestSupport partitionSupport() {
			return new PartitionTestSupport();
		}

	}

	private static class TestKinesisMessageChannelBinder extends KinesisMessageChannelBinder {

		TestKinesisMessageChannelBinder(KinesisAsyncClient amazonKinesis, DynamoDbAsyncClient dynamoDbClient,
				CloudWatchAsyncClient cloudWatchClient,
				KinesisBinderConfigurationProperties kinesisBinderConfigurationProperties,
				KinesisStreamProvisioner provisioningProvider) {

			super(kinesisBinderConfigurationProperties, provisioningProvider, amazonKinesis, dynamoDbClient,
					cloudWatchClient);
		}

		/*
		 * Some tests use multiple instance indexes for the same topic; we need to make the error infrastructure beans
		 * unique.
		 */
		@Override
		protected String errorsBaseName(ConsumerDestination destination, String group,
				ExtendedConsumerProperties<KinesisConsumerProperties> consumerProperties) {
			return super.errorsBaseName(destination, group, consumerProperties) + "-"
					+ consumerProperties.getInstanceIndex();
		}

		@Override
		protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
				ExtendedConsumerProperties<KinesisConsumerProperties> properties) {

			MessageProducer messageProducer = super.createConsumerEndpoint(destination, group, properties);
			if (messageProducer instanceof KinesisMessageDrivenChannelAdapter) {
				DirectFieldAccessor dfa = new DirectFieldAccessor(messageProducer);
				dfa.setPropertyValue("describeStreamBackoff", 10);
				dfa.setPropertyValue("consumerBackoff", 10);
				dfa.setPropertyValue("idleBetweenPolls", 1);
			}
			return messageProducer;
		}

	}

}
