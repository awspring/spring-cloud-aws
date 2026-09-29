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
package io.awspring.cloud.kinesis.support.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import tools.jackson.databind.json.JsonMapper;

class KinesisMessagingMessageConverterTests {

	@Test
	@DisplayName("concrete converter is a KinesisMessageConverter so it can be swapped through the SPI")
	void concreteConverterImplementsSpi() {
		KinesisMessageConverter converter = new KinesisMessagingMessageConverter();

		Message<byte[]> message = converter.toMessagingMessage(record("pk-1", "seq-1", "hello"), "shard-1",
				"my-stream");

		assertThat(message.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getHeaders().get(KinesisMessageHeaders.PARTITION_KEY)).isEqualTo("pk-1");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.SEQUENCE_NUMBER)).isEqualTo("seq-1");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.SHARD_ID)).isEqualTo("shard-1");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.STREAM_NAME)).isEqualTo("my-stream");
	}

	@Test
	@DisplayName("a user-supplied KinesisMessageConverter can be used through the interface reference")
	void userSuppliedConverterHonoredThroughInterface() {
		KinesisMessageConverter custom = new KinesisMessageConverter() {
			@Override
			public Message<byte[]> toMessagingMessage(KinesisClientRecord record, String shardId, String streamName) {
				return MessageBuilder.withPayload(bytes(record)).setHeader("custom", streamName).build();
			}

			@Override
			public byte[] fromMessagingMessage(Message<?> message) {
				return (byte[]) message.getPayload();
			}
		};

		Message<byte[]> message = custom.toMessagingMessage(record("pk-9", "seq-9", "payload"), "shard-9", "other");

		assertThat(message.getPayload()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getHeaders().get("custom")).isEqualTo("other");
	}

	@Test
	@DisplayName("content type is applied to the converted message headers when configured")
	void appliesContentTypeWhenConfigured() {
		KinesisMessagingMessageConverter converter = new KinesisMessagingMessageConverter();
		converter.setContentType(MimeTypeUtils.APPLICATION_JSON);

		Message<byte[]> message = converter.toMessagingMessage(record("pk-2", "seq-2", "{}"), "shard-2", "my-stream");

		assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	@DisplayName("fromMessagingMessage serializes payloads through the payload MessageConverter")
	void fromMessagingMessageSerializesPayload() {
		KinesisMessagingMessageConverter converter = new KinesisMessagingMessageConverter();
		converter.setPayloadMessageConverter(
				KinesisMessagingMessageConverter.defaultPayloadMessageConverter(new JsonMapper()));

		assertThat(converter.fromMessagingMessage(MessageBuilder.withPayload("hello").build()))
				.isEqualTo("hello".getBytes(StandardCharsets.UTF_8));

		byte[] raw = { 1, 2, 3 };
		assertThat(converter.fromMessagingMessage(MessageBuilder.withPayload(raw).build())).isEqualTo(raw);
	}

	private static byte[] bytes(KinesisClientRecord record) {
		ByteBuffer buffer = record.data().duplicate();
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return bytes;
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, String body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))).build();
	}

}
