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
package io.awspring.cloud.kinesis.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.schemaregistry.common.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.UserRecord;

/**
 *
 * @author Siddharth Jain
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
public class KplMessageHandlerTests {

	@Autowired
	protected Schema schema;

	@Autowired
	protected KinesisProducer kinesisProducer;

	@Autowired
	protected MessageChannel kinesisSendChannel;

	@Autowired
	protected KplMessageHandler kplMessageHandler;

	@Test
	void kplMessageHandlerWithRawPayloadBackpressureDisabledSuccess() {
		given(this.kinesisProducer.addUserRecord(any(UserRecord.class))).willReturn(mock());
		final Message<?> message = MessageBuilder.withPayload("someMessage")
				.setHeader(KinesisHeaders.PARTITION_KEY, "somePartitionKey")
				.setHeader(KinesisHeaders.SEQUENCE_NUMBER, "10").setHeader("someHeaderKey", "someHeaderValue").build();

		ArgumentCaptor<UserRecord> userRecordRequestArgumentCaptor = ArgumentCaptor.forClass(UserRecord.class);
		this.kplMessageHandler.setBackPressureThreshold(0);
		this.kinesisSendChannel.send(message);
		verify(this.kinesisProducer).addUserRecord(userRecordRequestArgumentCaptor.capture());
		verify(this.kinesisProducer, Mockito.never()).getOutstandingRecordsCount();
		UserRecord userRecord = userRecordRequestArgumentCaptor.getValue();
		assertThat(userRecord.getStreamName()).isEqualTo("someStream");
		assertThat(userRecord.getPartitionKey()).isEqualTo("somePartitionKey");
		assertThat(userRecord.getExplicitHashKey()).isNull();
		assertThat(userRecord.getSchema()).isSameAs(this.schema);
	}

	@Test
	void kplMessageHandlerWithRawPayloadBackpressureEnabledCapacityAvailable() {
		given(this.kinesisProducer.addUserRecord(any(UserRecord.class))).willReturn(mock());
		this.kplMessageHandler.setBackPressureThreshold(2);
		given(this.kinesisProducer.getOutstandingRecordsCount()).willReturn(1);
		final Message<?> message = MessageBuilder.withPayload("someMessage")
				.setHeader(KinesisHeaders.PARTITION_KEY, "somePartitionKey")
				.setHeader(KinesisHeaders.SEQUENCE_NUMBER, "10").setHeader("someHeaderKey", "someHeaderValue").build();

		ArgumentCaptor<UserRecord> userRecordRequestArgumentCaptor = ArgumentCaptor.forClass(UserRecord.class);

		this.kinesisSendChannel.send(message);
		verify(this.kinesisProducer).addUserRecord(userRecordRequestArgumentCaptor.capture());
		verify(this.kinesisProducer).getOutstandingRecordsCount();
		UserRecord userRecord = userRecordRequestArgumentCaptor.getValue();
		assertThat(userRecord.getStreamName()).isEqualTo("someStream");
		assertThat(userRecord.getPartitionKey()).isEqualTo("somePartitionKey");
		assertThat(userRecord.getExplicitHashKey()).isNull();
		assertThat(userRecord.getSchema()).isSameAs(this.schema);
	}

	@Test
	void kplMessageHandlerWithRawPayloadBackpressureEnabledCapacityInsufficient() {
		given(this.kinesisProducer.addUserRecord(any(UserRecord.class))).willReturn(mock());
		this.kplMessageHandler.setBackPressureThreshold(2);
		given(this.kinesisProducer.getOutstandingRecordsCount()).willReturn(5);
		final Message<?> message = MessageBuilder.withPayload("someMessage")
				.setHeader(KinesisHeaders.PARTITION_KEY, "somePartitionKey")
				.setHeader(KinesisHeaders.SEQUENCE_NUMBER, "10").setHeader("someHeaderKey", "someHeaderValue").build();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> this.kinesisSendChannel.send(message))
				.withCauseInstanceOf(MessageHandlingException.class)
				.withRootCauseExactlyInstanceOf(KplBackpressureException.class)
				.withStackTraceContaining("Cannot send record to Kinesis since buffer is at max capacity.");

		verify(this.kinesisProducer, Mockito.never()).addUserRecord(any(UserRecord.class));
		verify(this.kinesisProducer).getOutstandingRecordsCount();
	}

	@AfterEach
	public void tearDown() {
		clearInvocations(this.kinesisProducer);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public KinesisProducer kinesisProducer() {
			return mock();
		}

		@Bean
		public RequestHandlerRetryAdvice retryAdvice() {
			RequestHandlerRetryAdvice requestHandlerRetryAdvice = new RequestHandlerRetryAdvice();
			requestHandlerRetryAdvice.setRetryTemplate(RetryTemplate.builder().retryOn(KplBackpressureException.class)
					.exponentialBackoff(100, 2.0, 1000).maxAttempts(3).build());
			return requestHandlerRetryAdvice;
		}

		@Bean
		@ServiceActivator(inputChannel = "kinesisSendChannel", adviceChain = "retryAdvice")
		public MessageHandler kplMessageHandler(KinesisProducer kinesisProducer, Schema schema) {
			KplMessageHandler kplMessageHandler = new KplMessageHandler(kinesisProducer);
			kplMessageHandler.setAsync(true);
			kplMessageHandler.setStream("someStream");
			kplMessageHandler.setGlueSchema(schema);
			return kplMessageHandler;
		}

		@Bean
		public Schema schema() {
			return new Schema("syntax=\"proto2\";", "PROTOBUF", "testschema");
		}

	}

}
