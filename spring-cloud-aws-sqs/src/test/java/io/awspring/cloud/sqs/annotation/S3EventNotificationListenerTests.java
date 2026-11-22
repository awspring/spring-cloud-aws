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
package io.awspring.cloud.sqs.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Tests that {@link SqsListener @SqsListener} methods receive {@link S3EventNotification} payloads.
 *
 * @author Kassa
 */
class S3EventNotificationListenerTests {

	private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/s3-events";

	private static final String OBJECT_CREATED_EVENT = """
			{"Records":[{"eventVersion":"2.1","eventSource":"aws:s3","awsRegion":"us-east-1",
			"eventTime":"2026-11-21T12:00:00.000Z","eventName":"ObjectCreated:Put",
			"s3":{"s3SchemaVersion":"1.0","configurationId":"config",
			"bucket":{"name":"my-bucket","arn":"arn:aws:s3:::my-bucket"},
			"object":{"key":"reports/report.csv","size":1024,"eTag":"etag"}}}]}""";

	@Test
	void shouldReceiveS3EventNotification() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SqsConfiguration.class)) {
			S3EventNotification notification = context.getBean(Listener.class).received.get(10, TimeUnit.SECONDS);

			assertThat(notification.getRecords()).singleElement().satisfies(record -> {
				assertThat(record.getEventName()).isEqualTo("ObjectCreated:Put");
				assertThat(record.getS3().getObject().getKey()).isEqualTo("reports/report.csv");
			});
		}
	}

	private static SqsAsyncClient clientDelivering(String body) {
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		AtomicBoolean delivered = new AtomicBoolean();
		when(client.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(invocation -> {
			if (delivered.compareAndSet(false, true)) {
				Message message = Message.builder().messageId(UUID.randomUUID().toString())
						.receiptHandle("receipt-handle").body(body).build();
				return CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(message).build());
			}
			return CompletableFuture.supplyAsync(() -> ReceiveMessageResponse.builder().build(),
					CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
		});
		when(client.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(
				CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl(QUEUE_URL).build()));
		when(client.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(CompletableFuture.completedFuture(GetQueueAttributesResponse.builder().build()));
		when(client.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).thenAnswer(invocation -> {
			DeleteMessageBatchRequest request = invocation.getArgument(0);
			return CompletableFuture.completedFuture(DeleteMessageBatchResponse.builder()
					.successful(request.entries().stream()
							.map(entry -> DeleteMessageBatchResultEntry.builder().id(entry.id()).build()).toList())
					.build());
		});
		return client;
	}

	@Configuration
	@Import(SqsBootstrapConfiguration.class)
	static class SqsConfiguration {

		@Bean
		SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
			return SqsMessageListenerContainerFactory.builder().sqsAsyncClient(clientDelivering(OBJECT_CREATED_EVENT))
					.configure(options -> options.pollTimeout(Duration.ofSeconds(1))
							.listenerShutdownTimeout(Duration.ofSeconds(1))
							.acknowledgementShutdownTimeout(Duration.ofSeconds(1)))
					.build();
		}

		@Bean
		Listener listener() {
			return new Listener();
		}

	}

	static class Listener {

		final CompletableFuture<S3EventNotification> received = new CompletableFuture<>();

		@SqsListener(QUEUE_URL)
		void listen(S3EventNotification notification) {
			received.complete(notification);
		}

	}

}
