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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
public class KinesisMessagingMessageConverter implements KinesisMessageConverter {

	private final KinesisHeaderMapper headerMapper = new KinesisHeaderMapper();

	@Nullable
	private MimeType contentType;

	@Nullable
	private volatile MessageConverter payloadMessageConverter;

	public void setContentType(@Nullable MimeType contentType) {
		this.contentType = contentType;
	}

	/**
	 * Set the converter serializing payloads to {@code byte[]} in {@link #fromMessagingMessage(Message)}. Defaults to
	 * {@link #defaultPayloadMessageConverter(JsonMapper)} with a default {@link JsonMapper}.
	 * @param payloadMessageConverter the converter.
	 */
	public void setPayloadMessageConverter(MessageConverter payloadMessageConverter) {
		Assert.notNull(payloadMessageConverter, "payloadMessageConverter must not be null");
		this.payloadMessageConverter = payloadMessageConverter;
	}

	/**
	 * The converter serializing payloads, creating the default one on first use if none was set.
	 * @return the payload converter.
	 */
	public MessageConverter getPayloadMessageConverter() {
		MessageConverter converter = this.payloadMessageConverter;
		if (converter == null) {
			converter = defaultPayloadMessageConverter(new JsonMapper());
			this.payloadMessageConverter = converter;
		}
		return converter;
	}

	@Override
	public Message<byte[]> toMessagingMessage(KinesisClientRecord record, @Nullable String shardId, String streamName) {
		byte[] payload = toBytes(record.data());
		Map<String, Object> headers = this.headerMapper.toHeaders(record, shardId, streamName);
		if (this.contentType != null) {
			headers.put(MessageHeaders.CONTENT_TYPE, this.contentType);
		}
		return MessageBuilder.withPayload(payload).copyHeaders(headers).build();
	}

	@Override
	public byte[] fromMessagingMessage(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Object source = message.getPayload();
		Message<?> converted = getPayloadMessageConverter().toMessage(source, message.getHeaders());
		if (converted == null) {
			throw new MessageConversionException(message,
					"No payload converter could serialize a payload of type " + source.getClass().getName());
		}
		Object payload = converted.getPayload();
		if (!(payload instanceof byte[] bytes)) {
			throw new MessageConversionException(message, "The payload converter must produce a byte[] payload but "
					+ "produced " + payload.getClass().getName());
		}
		return bytes;
	}

	public static MessageConverter defaultPayloadMessageConverter(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper must not be null");
		return new CompositeMessageConverter(List.of(new ByteArrayMessageConverter(), new StringMessageConverter(),
				new JacksonJsonMessageConverter(jsonMapper)));
	}

	private static byte[] toBytes(@Nullable ByteBuffer data) {
		if (data == null || !data.hasRemaining()) {
			return new byte[0];
		}
		byte[] bytes = new byte[data.remaining()];
		data.get(data.position(), bytes);
		return bytes;
	}

}
