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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedAsyncClient;
import com.amazon.sqs.javamessaging.ExtendedAsyncClientConfiguration;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Matej Nedic
 */
@SpringBootTest
class SqsExtendedClientIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsExtendedClientIntegrationTests.class);

	static final String LARGE_PAYLOAD_QUEUE_NAME = "extended_client_large_payload_test_queue";

	static final String PAYLOAD_BUCKET_NAME = "extended-client-payload-bucket";

	// 300 KB > the 256 KB SQS message size limit, so the extended client offloads it to S3.
	static final String LARGE_PAYLOAD = "BEGIN-" + "x".repeat(300_000) + "-END";

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		S3AsyncClient s3Client = createS3AsyncClient();
		CompletableFuture.allOf(createQueue(client, LARGE_PAYLOAD_QUEUE_NAME),
				s3Client.createBucket(req -> req.bucket(PAYLOAD_BUCKET_NAME))).join();
	}

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@Test
	void receivesLargePayloadOffloadedToS3() throws Exception {
		sqsTemplate.send(LARGE_PAYLOAD_QUEUE_NAME, LARGE_PAYLOAD);
		logger.debug("Sent large payload of {} chars to {}", LARGE_PAYLOAD.length(), LARGE_PAYLOAD_QUEUE_NAME);

		assertThat(latchContainer.largePayloadLatch.await(20, TimeUnit.SECONDS)).isTrue();
		// the listener must receive the full payload, transparently rehydrated from S3
		assertThat(latchContainer.receivedPayload.get()).isEqualTo(LARGE_PAYLOAD);

		// prove the payload was actually offloaded to S3 (cleanup disabled, so the object persists after delete)
		S3AsyncClient s3Client = createS3AsyncClient();
		int objectCount = s3Client.listObjectsV2(req -> req.bucket(PAYLOAD_BUCKET_NAME)).get(10, TimeUnit.SECONDS)
				.contents().size();
		assertThat(objectCount).isGreaterThan(0);
	}

	static SqsAsyncClient createExtendedAsyncClient() {
		SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder().credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpoint()).region(Region.of(localstack.getRegion())).build();
		// cleanupS3Payload=false keeps the S3 object after the message is deleted, so the test can assert offloading
		ExtendedAsyncClientConfiguration config = new ExtendedAsyncClientConfiguration()
				.withPayloadSupportEnabled(createS3AsyncClient(), PAYLOAD_BUCKET_NAME, false);
		return new AmazonSQSExtendedAsyncClient(sqsAsyncClient, config);
	}

	static S3AsyncClient createS3AsyncClient() {
		return S3AsyncClient.builder().credentialsProvider(credentialsProvider)
				.endpointOverride(localstack.getEndpoint()).region(Region.of(localstack.getRegion()))
				.forcePathStyle(true).build();
	}

	static class LargePayloadListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = LARGE_PAYLOAD_QUEUE_NAME, id = "extended-client-large-payload")
		void listen(Message<String> message) {
			logger.debug("Received message with payload of {} chars", message.getPayload().length());
			latchContainer.receivedPayload.set(message.getPayload());
			latchContainer.largePayloadLatch.countDown();
		}

	}

	static class LatchContainer {

		final CountDownLatch largePayloadLatch = new CountDownLatch(1);

		final AtomicReference<String> receivedPayload = new AtomicReference<>();

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(
					options -> options.maxDelayBetweenPolls(Duration.ofSeconds(1)).pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(SqsExtendedClientIntegrationTests::createExtendedAsyncClient);
			return factory;
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(createExtendedAsyncClient()).build();
		}

		@Bean
		LargePayloadListener largePayloadListener() {
			return new LargePayloadListener();
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

	}

}
