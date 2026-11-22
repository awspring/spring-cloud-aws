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
package io.awspring.cloud.kinesis.operations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A record to send to Kinesis. The content is either a plain payload or a Spring {@link Message}, optionally combined
 * with custom headers.
 * <p>
 * When a {@link Builder#message(Message) message} is set, its headers are passed to the converter. A
 * {@link Builder#payload(Object) payload} set as well replaces the payload of the message and keeps its headers. Custom
 * {@link Builder#header(String, Object) headers} override message headers of the same name. Custom headers are only
 * visible on the wire when the converter embeds headers (see {@code EmbeddedHeadersKinesisMessageConverter}).
 * <p>
 * Instances are immutable: the custom headers are copied at build time.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class SendRequest {

	private final String streamName;

	private final String partitionKey;

	private final Object payload;

	@Nullable
	private final Message<?> message;

	private final Map<String, Object> headers;

	@Nullable
	private final String explicitHashKey;

	@Nullable
	private final String sequenceNumberForOrdering;

	private SendRequest(Builder builder) {
		Assert.hasText(builder.streamName, "streamName must not be empty");
		Assert.hasText(builder.partitionKey, "partitionKey must not be empty");
		Assert.isTrue(builder.payload != null || builder.message != null, "payload or message must be set");
		this.streamName = builder.streamName;
		this.partitionKey = builder.partitionKey;
		this.message = builder.message;
		this.payload = builder.payload != null ? builder.payload : builder.message.getPayload();
		this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
		this.explicitHashKey = builder.explicitHashKey;
		this.sequenceNumberForOrdering = builder.sequenceNumberForOrdering;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String streamName() {
		return this.streamName;
	}

	public String partitionKey() {
		return this.partitionKey;
	}

	public Object payload() {
		return this.payload;
	}

	/**
	 * The message to send, if the request was built from one.
	 * @return the message, or {@code null} for a payload request.
	 */
	@Nullable
	public Message<?> message() {
		return this.message;
	}

	/**
	 * The custom headers, not including the headers of {@link #message()}.
	 * @return an immutable map of custom headers.
	 */
	public Map<String, Object> headers() {
		return this.headers;
	}

	@Nullable
	public String explicitHashKey() {
		return this.explicitHashKey;
	}

	@Nullable
	public String sequenceNumberForOrdering() {
		return this.sequenceNumberForOrdering;
	}

	public static final class Builder {

		@Nullable
		private String streamName;

		@Nullable
		private String partitionKey;

		@Nullable
		private Object payload;

		@Nullable
		private Message<?> message;

		private final Map<String, Object> headers = new LinkedHashMap<>();

		@Nullable
		private String explicitHashKey;

		@Nullable
		private String sequenceNumberForOrdering;

		private Builder() {
		}

		public Builder streamName(String streamName) {
			this.streamName = streamName;
			return this;
		}

		public Builder partitionKey(String partitionKey) {
			this.partitionKey = partitionKey;
			return this;
		}

		public Builder payload(Object payload) {
			this.payload = payload;
			return this;
		}

		/**
		 * Set the message to send, keeping its headers.
		 * @param message the message.
		 * @return this builder.
		 */
		public Builder message(Message<?> message) {
			this.message = message;
			return this;
		}

		/**
		 * Add a custom header, overriding a message header of the same name.
		 * @param name the header name.
		 * @param value the header value.
		 * @return this builder.
		 */
		public Builder header(String name, Object value) {
			Assert.hasText(name, "header name must not be empty");
			Assert.notNull(value, "header value must not be null");
			this.headers.put(name, value);
			return this;
		}

		/**
		 * Add custom headers, overriding message headers of the same name.
		 * @param headers the headers.
		 * @return this builder.
		 */
		public Builder headers(Map<String, ?> headers) {
			Assert.notNull(headers, "headers must not be null");
			headers.forEach(this::header);
			return this;
		}

		public Builder explicitHashKey(@Nullable String explicitHashKey) {
			this.explicitHashKey = explicitHashKey;
			return this;
		}

		public Builder sequenceNumberForOrdering(@Nullable String sequenceNumberForOrdering) {
			this.sequenceNumberForOrdering = sequenceNumberForOrdering;
			return this;
		}

		public SendRequest build() {
			return new SendRequest(this);
		}

	}

}
