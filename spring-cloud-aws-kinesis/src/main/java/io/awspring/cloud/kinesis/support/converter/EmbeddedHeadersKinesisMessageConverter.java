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

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

/**
 * A {@link KinesisMessageConverter} that decodes and encodes headers embedded in the record data with Spring
 * Integration's {@link EmbeddedJsonHeadersMessageMapper}, the format used by the Kinesis binder and the Spring
 * Integration {@code KinesisMessageHandler}.
 * <p>
 * On read it delegates conversion to another converter and then decodes the resulting payload bytes. Records that do
 * not carry the embedded-headers envelope pass through unchanged. Embedded headers are copied onto the message only
 * when absent, so the Kinesis headers from the delegate win.
 * <p>
 * On write it delegates payload serialization and then embeds the message headers into the envelope.
 * <p>
 * This class references Spring Integration and must only be loaded when {@code spring-integration-core} is on the
 * classpath.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class EmbeddedHeadersKinesisMessageConverter implements KinesisMessageConverter {

	private final KinesisMessageConverter delegate;

	private final EmbeddedJsonHeadersMessageMapper headersMapper;

	/**
	 * Create a converter embedding every header except {@link MessageHeaders#ID} and {@link MessageHeaders#TIMESTAMP}.
	 * @param delegate the converter doing the payload conversion.
	 */
	public EmbeddedHeadersKinesisMessageConverter(KinesisMessageConverter delegate) {
		this(delegate, "!" + MessageHeaders.ID, "!" + MessageHeaders.TIMESTAMP, "*");
	}

	/**
	 * Create a converter embedding the headers matching the supplied patterns.
	 * @param delegate the converter doing the payload conversion.
	 * @param headerPatterns the header name patterns to embed, as understood by
	 *     {@link org.springframework.integration.support.utils.PatternMatchUtils}.
	 */
	public EmbeddedHeadersKinesisMessageConverter(KinesisMessageConverter delegate, String... headerPatterns) {
		Assert.notNull(delegate, "delegate must not be null");
		Assert.notNull(headerPatterns, "headerPatterns must not be null");
		this.delegate = delegate;
		this.headersMapper = new EmbeddedJsonHeadersMessageMapper(headerPatterns);
	}

	@Override
	public Message<byte[]> toMessagingMessage(KinesisClientRecord record, @Nullable String shardId, String streamName) {
		Message<byte[]> message = this.delegate.toMessagingMessage(record, shardId, streamName);
		Message<?> decoded = this.headersMapper.toMessage(message.getPayload(), null);
		Object payload = decoded.getPayload();
		if (!(payload instanceof byte[] bytes)) {
			return message;
		}
		MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes).copyHeaders(message.getHeaders());
		builder.copyHeadersIfAbsent(decoded.getHeaders());
		return builder.build();
	}

	@Override
	public byte[] fromMessagingMessage(Message<?> message) {
		byte[] payload = this.delegate.fromMessagingMessage(message);
		Map<String, Object> headers = message.getHeaders();
		return this.headersMapper.fromMessage(new GenericMessage<>(payload, headers));
	}

}
