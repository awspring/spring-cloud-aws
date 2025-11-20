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
package io.awspring.cloud.kinesis.stream.binder.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.awspring.cloud.kinesis.stream.binder.LocalstackContainerTest;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisBinderConfigurationProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisConsumerProperties;
import io.awspring.cloud.kinesis.stream.binder.properties.KinesisProducerProperties;
import java.time.Duration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;

/**
 * Tests for the {@link KinesisStreamProvisioner}.
 *
 * @author Jacob Severson
 * @author Artem Bilan
 * @author Sergiu Pantiru
 *
 * @since 4.0
 */
class KinesisStreamProvisionerTests implements LocalstackContainerTest {

	private static KinesisAsyncClient AMAZON_KINESIS;

	@BeforeAll
	static void setup() {
		AMAZON_KINESIS = LocalstackContainerTest.kinesisClient();
	}

	private void createStream(String streamName) {
		AMAZON_KINESIS.createStream(request -> request.streamName(streamName).shardCount(1))
				.thenCompose(reply -> AMAZON_KINESIS.waiter()
						.waitUntilStreamExists(request -> request.streamName(streamName), waiter -> waiter
								.maxAttempts(60).backoffStrategyV2(BackoffStrategy.fixedDelay(Duration.ofSeconds(1)))))
				.join();
	}

	@Test
	void testProvisionProducerSuccessfulWithExistingStream() {
		String streamName = "provisioning1";
		createStream(streamName);

		KinesisBinderConfigurationProperties binderProperties = new KinesisBinderConfigurationProperties();
		KinesisStreamProvisioner provisioner = new KinesisStreamProvisioner(AMAZON_KINESIS, binderProperties);
		ExtendedProducerProperties<KinesisProducerProperties> extendedProducerProperties = new ExtendedProducerProperties<>(
				new KinesisProducerProperties());

		ProducerDestination destination = provisioner.provisionProducerDestination(streamName,
				extendedProducerProperties);

		assertThat(destination.getName()).isEqualTo(streamName);
		assertThat(destination).isInstanceOf(KinesisProducerDestination.class).extracting("shards")
				.asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
	}

	@Test
	void testProvisionConsumerSuccessfulWithExistingStream() {
		String streamName = "provisioning2";
		createStream(streamName);

		KinesisBinderConfigurationProperties binderProperties = new KinesisBinderConfigurationProperties();
		KinesisStreamProvisioner provisioner = new KinesisStreamProvisioner(AMAZON_KINESIS, binderProperties);

		ExtendedConsumerProperties<KinesisConsumerProperties> extendedConsumerProperties = new ExtendedConsumerProperties<>(
				new KinesisConsumerProperties());
		extendedConsumerProperties.setHeaderMode(HeaderMode.embeddedHeaders);

		String group = "test-group";
		ConsumerDestination destination = provisioner.provisionConsumerDestination(streamName, group,
				extendedConsumerProperties);

		assertThat(destination.getName()).isEqualTo(streamName);
		assertThat(destination).isInstanceOf(KinesisConsumerDestination.class).extracting("shards")
				.asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
		assertThat(extendedConsumerProperties.getExtension().isEmbedHeaders()).isTrue();
		assertThat(extendedConsumerProperties.getHeaderMode()).isEqualTo(HeaderMode.none);
	}

	@Test
	void testProvisionConsumerExistingStreamUpdateShards() {
		String streamName = "provisioning3";
		createStream(streamName);

		KinesisBinderConfigurationProperties binderProperties = new KinesisBinderConfigurationProperties();
		int targetShardCount = 2;
		binderProperties.setMinShardCount(targetShardCount);
		binderProperties.setAutoAddShards(true);
		KinesisStreamProvisioner provisioner = new KinesisStreamProvisioner(AMAZON_KINESIS, binderProperties);

		ExtendedConsumerProperties<KinesisConsumerProperties> extendedConsumerProperties = new ExtendedConsumerProperties<>(
				new KinesisConsumerProperties());

		String group = "test-group";
		ConsumerDestination destination = provisioner.provisionConsumerDestination(streamName, group,
				extendedConsumerProperties);

		assertThat(destination.getName()).isEqualTo(streamName);
		assertThat(destination).isInstanceOf(KinesisConsumerDestination.class).extracting("shards")
				.asInstanceOf(InstanceOfAssertFactories.LIST).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	void testProvisionProducerSuccessfulWithNewStream() {
		String streamName = "provisioning4";
		KinesisBinderConfigurationProperties binderProperties = new KinesisBinderConfigurationProperties();
		KinesisStreamProvisioner provisioner = new KinesisStreamProvisioner(AMAZON_KINESIS, binderProperties);
		ExtendedProducerProperties<KinesisProducerProperties> extendedProducerProperties = new ExtendedProducerProperties<>(
				new KinesisProducerProperties());

		ProducerDestination destination = provisioner.provisionProducerDestination(streamName,
				extendedProducerProperties);

		assertThat(destination.getName()).isEqualTo(streamName);
		assertThat(destination).isInstanceOf(KinesisProducerDestination.class).extracting("shards")
				.asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1);
	}

	@Test
	void testProvisionConsumerResourceNotFoundException() {
		KinesisBinderConfigurationProperties binderProperties = new KinesisBinderConfigurationProperties();
		binderProperties.setAutoCreateStream(false);
		KinesisStreamProvisioner provisioner = new KinesisStreamProvisioner(AMAZON_KINESIS, binderProperties);
		int instanceCount = 1;
		int concurrency = 1;

		ExtendedConsumerProperties<KinesisConsumerProperties> extendedConsumerProperties = new ExtendedConsumerProperties<>(
				new KinesisConsumerProperties());
		extendedConsumerProperties.setInstanceCount(instanceCount);
		extendedConsumerProperties.setConcurrency(concurrency);

		String name = "provisioning5";
		String group = "test-group";

		assertThatExceptionOfType(ProvisioningException.class)
				.isThrownBy(() -> provisioner.provisionConsumerDestination(name, group, extendedConsumerProperties))
				.withMessageContaining("The stream [provisioning5] was not found and auto creation is disabled.")
				.withCauseInstanceOf(ResourceNotFoundException.class);
	}

}
