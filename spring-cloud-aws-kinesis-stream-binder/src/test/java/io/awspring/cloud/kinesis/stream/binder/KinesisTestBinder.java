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
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionTestSupport;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import software.amazon.awssdk.retries.api.BackoffStrategy;
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

	private final Set<String> provisionedStreams = ConcurrentHashMap.newKeySet();

	public KinesisTestBinder(KinesisAsyncClient amazonKinesis, DynamoDbAsyncClient dynamoDbClient,
			CloudWatchAsyncClient cloudWatchClient,
			KinesisBinderConfigurationProperties kinesisBinderConfigurationProperties) {

		this.applicationContext = new AnnotationConfigApplicationContext(Config.class);

		this.amazonKinesis = amazonKinesis;

		KinesisStreamProvisioner provisioningProvider = new RecordingProvisioner(amazonKinesis,
				kinesisBinderConfigurationProperties, this.provisionedStreams);

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
		// Delete only the streams this binder provisioned. Listing the account and deleting everything
		// removes streams that other test classes are still using, which is invisible while the classes run
		// one at a time and fails them with ResourceNotFoundException as soon as they do not.
		CompletableFuture.allOf(this.provisionedStreams.stream().map(streamName -> this.amazonKinesis
				.deleteStream(request -> request.streamName(streamName))
				// The SDK default waiter backs off in flat 10-second steps, so every
				// stream deletion costs at least 10 seconds. Poll once per second.
				.thenCompose(result -> this.amazonKinesis.waiter().waitUntilStreamNotExists(
						request -> request.streamName(streamName),
						waiter -> waiter.maxAttempts(60)
								.backoffStrategyV2(BackoffStrategy.fixedDelayWithoutJitter(Duration.ofSeconds(1)))))
				.exceptionally(throwable -> null)).toArray(CompletableFuture[]::new)).join();
		this.provisionedStreams.clear();
	}

	private static final class RecordingProvisioner extends KinesisStreamProvisioner {

		/**
		 * Localstack only creates a few streams at a time, and the provisioner creates one per binding, so concurrent
		 * classes can exceed that limit. Gate provisioning rather than retry it: CreateStream is not idempotent and
		 * answers ResourceInUseException once the stream exists.
		 */
		private static final Semaphore PROVISIONING = new Semaphore(3);

		private final Set<String> provisionedStreams;

		RecordingProvisioner(KinesisAsyncClient amazonKinesis,
				KinesisBinderConfigurationProperties configurationProperties, Set<String> provisionedStreams) {
			super(amazonKinesis, configurationProperties);
			this.provisionedStreams = provisionedStreams;
		}

		@Override
		public ProducerDestination provisionProducerDestination(String name,
				ExtendedProducerProperties<KinesisProducerProperties> properties) {
			this.provisionedStreams.add(name);
			PROVISIONING.acquireUninterruptibly();
			try {
				return super.provisionProducerDestination(name, properties);
			}
			finally {
				PROVISIONING.release();
			}
		}

		@Override
		public ConsumerDestination provisionConsumerDestination(String name, String group,
				ExtendedConsumerProperties<KinesisConsumerProperties> properties) {
			this.provisionedStreams.add(name);
			PROVISIONING.acquireUninterruptibly();
			try {
				return super.provisionConsumerDestination(name, group, properties);
			}
			finally {
				PROVISIONING.release();
			}
		}

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

			super(kinesisBinderConfigurationProperties, provisioningProvider, amazonKinesis, dynamoDbClient, null,
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
