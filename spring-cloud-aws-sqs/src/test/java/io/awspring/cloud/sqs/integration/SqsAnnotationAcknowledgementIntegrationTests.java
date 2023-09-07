/*
 * Copyright 2013-2023 the original author or authors.
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

import static java.util.Collections.singletonMap;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.ContainerComponentFactory;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.StandardSqsComponentFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementExecutor;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.SqsAcknowledgementExecutor;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.source.AbstractSqsMessageSource;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@SpringBootTest
@TestPropertySource(properties = { "test.property.acknowledgement=ALWAYS" })
public class SqsAnnotationAcknowledgementIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsAnnotationAcknowledgementIntegrationTests.class);

	static final String ACK_AFTER_SECOND_ERROR_FACTORY = "ackAfterSecondErrorFactory";

	static final String ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME = "annotation_always_ack_success_test_queue";

	static final String ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME = "annotation_always_ack_error_test_queue";

	static final String ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME = "annotation_on_success_ack_success_test_queue";

	static final String ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME = "annotation_on_success_ack_error_test_queue";

	static final String ANNOTATION_MANUAL_ACK_QUEUE_NAME = "annotation_ack_manual_test_queue";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME),
				createQueue(client, ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME),
				createQueue(client, ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME),
				createQueue(client, ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME,
						singletonMap(QueueAttributeName.VISIBILITY_TIMEOUT, "1")),
				createQueue(client, ANNOTATION_MANUAL_ACK_QUEUE_NAME)).join();
	}

	@Test
	void annotationAlwaysAckOnSuccess() throws Exception {
		String messageBody = "annotationAlwaysAckOnSuccess-payload";
		sqsTemplate.send(ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME,
				messageBody);
		assertThat(latchContainer.annotationAlwaysAckSuccessLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void annotationAlwaysAckOnError() throws Exception {
		String messageBody = "annotationAlwaysAckOnError-payload";
		sqsTemplate.send(ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME,
				messageBody);
		assertThat(latchContainer.annotationAlwaysAckErrorLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void annotationOnSuccessAckOnSuccess() throws Exception {
		String messageBody = "annotationOnSuccessAckOnSuccess-payload";
		sqsTemplate.send(ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME,
				messageBody);
		assertThat(latchContainer.annotationOnSuccessAckSuccessLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void annotationOnSuccessAckOnError() throws Exception {
		String messageBody = "annotationOnSuccessAckOnError-payload";
		sqsTemplate.send(ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME,
				messageBody);
		assertThat(latchContainer.annotationOnSuccessAckErrorLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.annotationOnSuccessAckErrorCallbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void annotationManualAck() throws Exception {
		String messageBody = "annotationManualAck-payload";
		sqsTemplate.send(ANNOTATION_MANUAL_ACK_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", ANNOTATION_MANUAL_ACK_QUEUE_NAME, messageBody);
		assertThat(latchContainer.annotationManualAckLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latchContainer.annotationManualAckLatchCallback.await(10, TimeUnit.SECONDS)).isFalse();
	}

	static class AnnotationAlwaysAcknowledgeOnSuccessMessagesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME, id = "annotation-always-ack-success", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.annotationAlwaysAckSuccessLatch.countDown();
		}

	}

	static class AnnotationAlwaysAcknowledgeOnErrorMessagesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME, id = "annotation-always-ack-error", acknowledgementMode = "${test.property.acknowledgement}")
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.annotationAlwaysAckErrorLatch.countDown();
			throw new RuntimeException("Expected exception from annotation-ack-always-error");
		}

	}

	static class AnnotationOnSuccessAcknowledgeOnSuccessMessagesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME, factory = ACK_AFTER_SECOND_ERROR_FACTORY, id = "annotation-onsuccess-ack-success", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.annotationOnSuccessAckSuccessLatch.countDown();
		}

	}

	static class AnnotationOnSuccessAcknowledgeOnErrorMessagesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME, factory = ACK_AFTER_SECOND_ERROR_FACTORY, id = "annotation-onsuccess-ack-error", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.annotationOnSuccessAckErrorLatch.countDown();
			throw new RuntimeException("Expected exception from annotation-onsuccess-ack-error");
		}

	}

	static class AnnotationManualAcknowledgeMessagesListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = ANNOTATION_MANUAL_ACK_QUEUE_NAME, id = "annotation-manual-ack", acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL)
		void listen(String message) {
			logger.debug("Received message in Listener Method: " + message);
			latchContainer.annotationManualAckLatch.countDown();
		}

	}

	static class LatchContainer {

		final CountDownLatch annotationAlwaysAckSuccessLatch = new CountDownLatch(2);

		final CountDownLatch annotationAlwaysAckErrorLatch = new CountDownLatch(2);

		final CountDownLatch annotationOnSuccessAckSuccessLatch = new CountDownLatch(2);

		final CountDownLatch annotationOnSuccessAckErrorLatch = new CountDownLatch(1);

		final CountDownLatch annotationOnSuccessAckErrorCallbackLatch = new CountDownLatch(1);

		final CountDownLatch annotationManualAckLatch = new CountDownLatch(1);

		final CountDownLatch annotationManualAckLatchCallback = new CountDownLatch(1);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}

		@Bean
		AnnotationAlwaysAcknowledgeOnSuccessMessagesListener annotationAlwaysAcknowledgeOnSuccessMessagesListener() {
			return new AnnotationAlwaysAcknowledgeOnSuccessMessagesListener();
		}

		@Bean
		AnnotationAlwaysAcknowledgeOnErrorMessagesListener annotationAlwaysAcknowledgeOnErrorMessagesListener() {
			return new AnnotationAlwaysAcknowledgeOnErrorMessagesListener();
		}

		@Bean
		AnnotationOnSuccessAcknowledgeOnSuccessMessagesListener annotationOnSuccessAcknowledgeOnSuccessMessagesListener() {
			return new AnnotationOnSuccessAcknowledgeOnSuccessMessagesListener();
		}

		@Bean
		AnnotationOnSuccessAcknowledgeOnErrorMessagesListener annotationOnSuccessAcknowledgeOnErrorMessagesListener() {
			return new AnnotationOnSuccessAcknowledgeOnErrorMessagesListener();
		}

		@Bean
		AnnotationManualAcknowledgeMessagesListener annotationManualAcknowledgeMessagesListener() {
			return new AnnotationManualAcknowledgeMessagesListener();
		}

		@Bean
		public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory() {
			return SqsMessageListenerContainerFactory.builder()
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient)
					.acknowledgementResultCallback(getAcknowledgementResultCallback())
					.configure(options -> options.maxDelayBetweenPolls(Duration.ofSeconds(5))
							.queueAttributeNames(Collections.singletonList(QueueAttributeName.QUEUE_ARN))
							.pollTimeout(Duration.ofSeconds(5)))
					.build();
		}

		@Bean(name = ACK_AFTER_SECOND_ERROR_FACTORY)
		public SqsMessageListenerContainerFactory<Object> ackAfterSecondErrorFactory() {
			return SqsMessageListenerContainerFactory.builder()
					.configure(options -> options.maxConcurrentMessages(10).pollTimeout(Duration.ofSeconds(10))
							.maxMessagesPerPoll(10).maxDelayBetweenPolls(Duration.ofSeconds(1)))
					.containerComponentFactories(getExceptionThrowingAckExecutor())
					.acknowledgementResultCallback(getAcknowledgementResultCallback()).errorHandler(testErrorHandler())
					.sqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient).build();
		}

		private List<ContainerComponentFactory<Object, SqsContainerOptions>> getExceptionThrowingAckExecutor() {
			return Collections.singletonList(new StandardSqsComponentFactory<Object>() {
				@Override
				public MessageSource<Object> createMessageSource(SqsContainerOptions options) {
					return new AbstractSqsMessageSource<Object>() {
						@Override
						protected AcknowledgementExecutor<Object> createAcknowledgementExecutorInstance() {
							return new SqsAcknowledgementExecutor<Object>() {

								final AtomicBoolean hasThrown = new AtomicBoolean();

								@Override
								public CompletableFuture<Void> execute(Collection<Message<Object>> messagesToAck) {
									final String queueName = MessageHeaderUtils.getHeaderAsString(
											messagesToAck.iterator().next(), SqsHeaders.SQS_QUEUE_NAME_HEADER);
									if (queueName.equals(ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME)
											&& hasThrown.compareAndSet(false, true)) {
										return CompletableFutures.failedFuture(new RuntimeException(
												"Expected acknowledgement exception for " + queueName));
									}
									return super.execute(messagesToAck);
								}
							};
						}
					};
				}
			});
		}

		private AcknowledgementResultCallback<Object> getAcknowledgementResultCallback() {
			return new AcknowledgementResultCallback<>() {
				@Override
				public void onSuccess(Collection<Message<Object>> messages) {
					logger.debug("Invoking on success acknowledgement result callback for {}",
							MessageHeaderUtils.getId(messages));
					final String queueName = MessageHeaderUtils.getHeaderAsString(messages.iterator().next(),
							SqsHeaders.SQS_QUEUE_NAME_HEADER);

					final Map<String, CountDownLatch> latches = Map.ofEntries(
							entry(ANNOTATION_ALWAYS_ACK_SUCCESS_QUEUE_NAME,
									latchContainer.annotationAlwaysAckSuccessLatch),
							entry(ANNOTATION_ALWAYS_ACK_ERROR_QUEUE_NAME, latchContainer.annotationAlwaysAckErrorLatch),
							entry(ANNOTATION_ON_SUCCESS_ACK_SUCCESS_QUEUE_NAME,
									latchContainer.annotationOnSuccessAckSuccessLatch),
							entry(ANNOTATION_MANUAL_ACK_QUEUE_NAME, latchContainer.annotationManualAckLatchCallback));

					if (latches.containsKey(queueName)) {
						latches.get(queueName).countDown();
					}
				}

				@Override
				public void onFailure(Collection<Message<Object>> messages, Throwable t) {
					logger.debug("Invoking on failure acknowledgement result callback for {}",
							MessageHeaderUtils.getId(messages));
					final String queueName = MessageHeaderUtils.getHeaderAsString(messages.iterator().next(),
							SqsHeaders.SQS_QUEUE_NAME_HEADER);

					final Map<String, CountDownLatch> latches = Map
							.ofEntries(entry(ANNOTATION_ON_SUCCESS_ACK_ERROR_QUEUE_NAME,
									latchContainer.annotationOnSuccessAckErrorCallbackLatch));

					if (latches.containsKey(queueName)) {
						latches.get(queueName).countDown();
					}
				}
			};
		}

		private AsyncErrorHandler<Object> testErrorHandler() {
			return new AsyncErrorHandler<Object>() {

				final List<Object> previousMessages = Collections.synchronizedList(new ArrayList<>());

				@Override
				public CompletableFuture<Void> handle(Message<Object> message, Throwable t) {
					// Eventually ack to not interfere with other tests.
					if (previousMessages.contains(message.getPayload())) {
						return CompletableFuture.completedFuture(null);
					}
					previousMessages.add(message.getPayload());
					return CompletableFutures.failedFuture(t);
				}

				@Override
				public CompletableFuture<Void> handle(Collection<Message<Object>> messages, Throwable t) {
					// Eventually ack to not interfere with other tests.
					if (previousMessages.containsAll(toPayloadList(messages))) {
						return CompletableFuture.completedFuture(null);
					}
					previousMessages.addAll(toPayloadList(messages));
					return CompletableFutures.failedFuture(t);
				}

				private List<Object> toPayloadList(Collection<Message<Object>> messages) {
					return messages.stream().map(Message::getPayload).collect(Collectors.toList());
				}

				private Collection<DeleteMessageBatchRequestEntry> getBatchEntries(
						Collection<Message<Object>> messages) {
					return messages.stream().map(this::getBatchEntry).collect(Collectors.toList());
				}

				private DeleteMessageBatchRequestEntry getBatchEntry(Message<Object> message) {
					return DeleteMessageBatchRequestEntry.builder().id(UUID.randomUUID().toString())
							.receiptHandle(
									MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.SQS_RECEIPT_HANDLE_HEADER))
							.build();
				}
			};
		}

	}

}
