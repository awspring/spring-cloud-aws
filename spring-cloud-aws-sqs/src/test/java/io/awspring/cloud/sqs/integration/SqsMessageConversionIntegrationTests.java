/*
 * Copyright 2013-2024 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SnsNotificationMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageHeaders;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * Integration tests for SQS message conversion.
 *
 * @author Tomaz Fernandes
 * @author Mikhail Strokov
 * @author Dongha Kim
 * @author Wei Jiang
 */
@SpringBootTest
class SqsMessageConversionIntegrationTests extends BaseSqsIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageConversionIntegrationTests.class);

	static final String RESOLVES_POJO_TYPES_QUEUE_NAME = "resolves_pojo_test_queue";
	static final String RESOLVES_POJO_MESSAGE_QUEUE_NAME = "resolves_pojo_message_test_queue";
	static final String RESOLVES_POJO_LIST_QUEUE_NAME = "resolves_pojo_list_test_queue";
	static final String RESOLVES_POJO_MESSAGE_LIST_QUEUE_NAME = "resolves_pojo_message_list_test_queue";
	static final String RESOLVES_POJO_FROM_HEADER_QUEUE_NAME = "resolves_pojo_from_mapping_test_queue";
	static final String RESOLVES_MY_OTHER_POJO_FROM_HEADER_QUEUE_NAME = "resolves_my_other_pojo_from_mapping_test_queue";
	static final String RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME = "resolves_pojo_from_notification_message_queue";
	static final String RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME = "resolves_pojo_from_notification_message_list_test_queue";

	@Autowired
	LatchContainer latchContainer;

	@Autowired
	SqsTemplate sqsTemplate;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, RESOLVES_POJO_TYPES_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_MESSAGE_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_LIST_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_MESSAGE_LIST_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_FROM_HEADER_QUEUE_NAME),
				createQueue(client, RESOLVES_MY_OTHER_POJO_FROM_HEADER_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME),
				createQueue(client, RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME)).join();
	}

	@Test
	void resolvesPojoParameterTypes() throws Exception {
		MyPojo messageBody = new MyPojo("pojoParameterType", "secondValue");
		sqsTemplate.send(RESOLVES_POJO_TYPES_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_TYPES_QUEUE_NAME, messageBody);
		assertThat(latchContainer.resolvesPojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesPojoMessage() throws Exception {
		MyPojo messageBody = new MyPojo("resolvesPojoMessage", "secondValue");
		sqsTemplate.send(RESOLVES_POJO_MESSAGE_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_MESSAGE_QUEUE_NAME, messageBody);
		assertThat(latchContainer.resolvesPojoMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesPojoList() throws Exception {
		MyPojo payload = new MyPojo("resolvesPojoList", "secondValue");
		sqsTemplate.send(RESOLVES_POJO_LIST_QUEUE_NAME, payload);
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_LIST_QUEUE_NAME, payload);
		assertThat(latchContainer.resolvesPojoListLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesPojoMessageList() throws Exception {
		MyPojo messageBody = new MyPojo("resolvesPojoMessageList", "secondValue");
		sqsTemplate.send(RESOLVES_POJO_MESSAGE_LIST_QUEUE_NAME, messageBody);
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_MESSAGE_LIST_QUEUE_NAME,
				messageBody);
		assertThat(latchContainer.resolvesPojoMessageListLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesPojoFromHeader() throws Exception {
		MyPojo payload = new MyPojo("pojoParameterType", "secondValue");
		sqsTemplate.send(RESOLVES_POJO_FROM_HEADER_QUEUE_NAME,
				MessageBuilder.createMessage(payload, new MessagingMessageHeaders(getHeaderMapping(MyPojo.class))));
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_FROM_HEADER_QUEUE_NAME, payload);
		assertThat(latchContainer.resolvesPojoFromMappingLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesMyOtherPojoFromHeader() throws Exception {
		MyOtherPojo payload = new MyOtherPojo("pojoParameterType", "secondValue");
		sqsTemplate.send(RESOLVES_MY_OTHER_POJO_FROM_HEADER_QUEUE_NAME, MessageBuilder.createMessage(payload,
				new MessagingMessageHeaders(getHeaderMapping(MyOtherPojo.class))));
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_MY_OTHER_POJO_FROM_HEADER_QUEUE_NAME,
				payload);
		assertThat(latchContainer.resolvesMyOtherPojoFromMappingLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesMyPojoFromNotificationMessage() throws Exception {
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));
		String payload = new String(notificationJsonContent);
		sqsTemplate.send(RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME, payload);
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME,
				payload);
		assertThat(latchContainer.resolvesPojoNotificationMessageLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void resolvesMyPojoFromNotificationMessageList() throws Exception {
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));
		String payload = new String(notificationJsonContent);
		List<Message<String>> messages = IntStream.range(0, 10)
				.mapToObj(index -> MessageBuilder.withPayload(payload).build()).toList();
		sqsTemplate.sendMany(RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME, messages);
		logger.debug("Sent message to queue {} with messageBody {}",
				RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME, payload);
		assertThat(latchContainer.resolvesPojoNotificationMessageListLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	private Map<String, Object> getHeaderMapping(Class<?> clazz) {
		return Collections.singletonMap(SqsHeaders.SQS_DEFAULT_TYPE_HEADER, clazz.getName());
	}

	@Test
	void shouldSendAndReceiveJsonString() throws Exception {
		String messageBody = """
				{
				  "firstField": "hello",
				  "secondField": "sqs!"
				}
				""";
		sqsTemplate.send(to -> to.queue(RESOLVES_POJO_TYPES_QUEUE_NAME).payload(messageBody)
				.header(MessageHeaders.CONTENT_TYPE, "application/json"));
		logger.debug("Sent message to queue {} with messageBody {}", RESOLVES_POJO_TYPES_QUEUE_NAME, messageBody);
		assertThat(latchContainer.resolvesPojoLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	static class ResolvesPojoListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_TYPES_QUEUE_NAME, id = "resolves-pojo")
		void listen(MyPojo pojo, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Assert.notNull(pojo.firstField, "Received null message");
			logger.debug("Received message {} from queue {}", pojo, queueName);
			latchContainer.resolvesPojoLatch.countDown();
		}
	}

	static class ResolvesPojoMessageListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_MESSAGE_QUEUE_NAME, id = "resolves-pojo-message")
		void listen(Message<MyPojo> pojo, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Assert.notNull(pojo.getPayload().firstField, "Received null message");
			logger.debug("Received message {} from queue {}", pojo, queueName);
			latchContainer.resolvesPojoMessageLatch.countDown();
		}
	}

	static class ResolvesPojoListListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_LIST_QUEUE_NAME, id = "resolves-pojo-list")
		void listen(List<MyPojo> pojos) {
			Assert.notNull(pojos.get(0).firstField, "Received null message");
			logger.debug("Received messages {} from queue {}", pojos, RESOLVES_POJO_MESSAGE_QUEUE_NAME);
			latchContainer.resolvesPojoListLatch.countDown();
		}
	}

	static class ResolvesPojoMessageListListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_MESSAGE_LIST_QUEUE_NAME, id = "resolves-pojo-message-list")
		void listen(List<Message<MyPojo>> pojos) {
			Assert.notNull(pojos.get(0).getPayload().firstField, "Received null message");
			logger.debug("Received messages {} from queue {}", pojos, RESOLVES_POJO_MESSAGE_QUEUE_NAME);
			latchContainer.resolvesPojoMessageListLatch.countDown();
		}
	}

	static class ResolvesMyPojoWithMappingListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_FROM_HEADER_QUEUE_NAME, id = "resolves-pojo-with-mapping", factory = "myPojoListenerContainerFactory")
		void listen(Message<MyInterface> pojo, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Assert.isInstanceOf(MyPojo.class, pojo.getPayload());
			Assert.notNull(((MyPojo) pojo.getPayload()).firstField, "Received null message");
			logger.debug("Received message {} from queue {}", pojo, queueName);
			latchContainer.resolvesPojoLatch.countDown();
		}
	}

	static class ResolvesMyOtherPojoWithMappingListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_MY_OTHER_POJO_FROM_HEADER_QUEUE_NAME, id = "resolves-my-other-pojo-with-mapping", factory = "myPojoListenerContainerFactory")
		void listen(MyInterface pojo, @Header(SqsHeaders.SQS_QUEUE_NAME_HEADER) String queueName) {
			Assert.isInstanceOf(MyOtherPojo.class, pojo);
			Assert.notNull(((MyOtherPojo) pojo).otherFirstField, "Received null message");
			logger.debug("Received message {} from queue {}", pojo, queueName);
			latchContainer.resolvesPojoMessageLatch.countDown();
		}
	}

	static class ResolvesPojoWithNotificationAnnotationListener {

		@Autowired
		LatchContainer latchContainer;

		@SqsListener(queueNames = RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME, id = "resolves-pojo-with-notification-message", factory = "defaultSqsListenerContainerFactory")
		void listen(@SnsNotificationMessage MyEnvelope<MyPojo> myPojo) {
			assertThat(myPojo.getData().getFirstField()).isEqualTo("pojoNotificationMessage");
			logger.debug("Received message {} from queue {}", myPojo,
					RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_QUEUE_NAME);
			latchContainer.resolvesPojoNotificationMessageLatch.countDown();
		}

		@SqsListener(queueNames = RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME, id = "resolves-pojo-with-notification-message-list", factory = "defaultSqsListenerContainerFactory")
		void listen(@SnsNotificationMessage List<MyEnvelope<MyPojo>> myPojos) {
			Assert.notEmpty(myPojos, "Received empty messages");
			logger.debug("Received messages {} from queue {}", myPojos,
					RESOLVES_POJO_FROM_NOTIFICATION_MESSAGE_LIST_QUEUE_NAME);

			for (MyEnvelope<MyPojo> myPojo : myPojos) {
				assertThat(myPojo.getData().getFirstField()).isEqualTo("pojoNotificationMessage");
			}
			latchContainer.resolvesPojoNotificationMessageListLatch.countDown();
		}
	}

	static class LatchContainer {

		CountDownLatch resolvesPojoLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoMessageLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoListLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoMessageListLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoFromMappingLatch = new CountDownLatch(1);
		CountDownLatch resolvesMyOtherPojoFromMappingLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoNotificationMessageLatch = new CountDownLatch(1);
		CountDownLatch resolvesPojoNotificationMessageListLatch = new CountDownLatch(1);

	}

	@Import(SqsBootstrapConfiguration.class)
	@Configuration
	static class SQSConfiguration {

		// @formatter:off
		@Bean
		public SqsMessageListenerContainerFactory<String> defaultSqsListenerContainerFactory() {
			SqsMessageListenerContainerFactory<String> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
				.maxDelayBetweenPolls(Duration.ofSeconds(1))
				.pollTimeout(Duration.ofSeconds(3)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			return factory;
		}

		@Bean
		public SqsMessageListenerContainerFactory<MyInterface> myPojoListenerContainerFactory() {
			SqsMessageListenerContainerFactory<MyInterface> factory = new SqsMessageListenerContainerFactory<>();
			factory.configure(options -> options
					.queueAttributeNames(Collections.singletonList(QueueAttributeName.VISIBILITY_TIMEOUT))
					.maxDelayBetweenPolls(Duration.ofSeconds(1))
					.pollTimeout(Duration.ofSeconds(1)));
			factory.setSqsAsyncClientSupplier(BaseSqsIntegrationTest::createAsyncClient);
			factory.addMessageInterceptor(new AsyncMessageInterceptor<MyInterface>() {
				@Override
				public CompletableFuture<Message<MyInterface>> intercept(Message<MyInterface> message) {
					MyInterface payload = message.getPayload();
					Assert.notNull(payload, "null payload");
					if (payload instanceof MyPojo) {
						Assert.notNull(((MyPojo) payload).firstField, "null firstField");
						latchContainer.resolvesPojoFromMappingLatch.countDown();
					}
					else if (payload instanceof MyOtherPojo) {
						Assert.notNull(((MyOtherPojo) payload).otherFirstField, "null otherFirstField");
						latchContainer.resolvesMyOtherPojoFromMappingLatch.countDown();
					}
					return CompletableFuture.completedFuture(message);
				}
			});
			return factory;
		}

		// @formatter:on

		LatchContainer latchContainer = new LatchContainer();

		@Bean
		ResolvesPojoListener resolvesPojoListener() {
			return new ResolvesPojoListener();
		}

		@Bean
		ResolvesPojoListListener resolvesPojoListListener() {
			return new ResolvesPojoListListener();
		}

		@Bean
		ResolvesPojoMessageListener resolvesPojoMessageListener() {
			return new ResolvesPojoMessageListener();
		}

		@Bean
		ResolvesPojoMessageListListener resolvesPojoMessageListListener() {
			return new ResolvesPojoMessageListListener();
		}

		@Bean
		ResolvesMyPojoWithMappingListener resolvesMyPojoWithMappingListener() {
			return new ResolvesMyPojoWithMappingListener();
		}

		@Bean
		ResolvesMyOtherPojoWithMappingListener resolvesMyOtherPojoWithMappingListener() {
			return new ResolvesMyOtherPojoWithMappingListener();
		}

		@Bean
		ResolvesPojoWithNotificationAnnotationListener resolvesPojoWithNotificationAnnotationListener() {
			return new ResolvesPojoWithNotificationAnnotationListener();
		}

		@Bean
		LatchContainer latchContainer() {
			return this.latchContainer;
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		SqsListenerConfigurer customizer(ObjectMapper objectMapper) {
			return registrar -> registrar.setObjectMapper(objectMapper);
		}

		@Bean
		SqsTemplate sqsTemplate() {
			return SqsTemplate.builder().sqsAsyncClient(BaseSqsIntegrationTest.createAsyncClient()).build();
		}

	}

	static class MyEnvelope<T> {
		String specversion;
		T data;

		public String getSpecversion() {
			return specversion;
		}

		public void setSpecversion(String specversion) {
			this.specversion = specversion;
		}

		public T getData() {
			return data;
		}

		public void setData(T data) {
			this.data = data;
		}
	}

	static class MyPojo implements MyInterface {

		String firstField;
		String secondField;

		MyPojo(String firstField, String secondField) {
			this.firstField = firstField;
			this.secondField = secondField;
		}

		MyPojo() {
		}

		public String getFirstField() {
			return firstField;
		}

		public void setFirstField(String firstField) {
			this.firstField = firstField;
		}

		public String getSecondField() {
			return secondField;
		}

		public void setSecondField(String secondField) {
			this.secondField = secondField;
		}

	}

	static class MyOtherPojo implements MyInterface {

		String otherFirstField;
		String otherSecondField;

		MyOtherPojo(String otherFirstField, String otherSecondField) {
			this.otherFirstField = otherFirstField;
			this.otherSecondField = otherSecondField;
		}

		MyOtherPojo() {
		}

		public String getOtherFirstField() {
			return otherFirstField;
		}

		public void setOtherFirstField(String otherFirstField) {
			this.otherFirstField = otherFirstField;
		}

		public String getOtherSecondField() {
			return otherSecondField;
		}

		public void setOtherSecondField(String otherSecondField) {
			this.otherSecondField = otherSecondField;
		}

	}

	interface MyInterface {
	}

}
