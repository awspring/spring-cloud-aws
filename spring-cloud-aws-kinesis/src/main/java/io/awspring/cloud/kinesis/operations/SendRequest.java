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

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public final class SendRequest {

	private final String streamName;

	private final String partitionKey;

	private final Object payload;

	@Nullable
	private final String explicitHashKey;

	@Nullable
	private final String sequenceNumberForOrdering;

	private SendRequest(Builder builder) {
		Assert.hasText(builder.streamName, "streamName must not be empty");
		Assert.hasText(builder.partitionKey, "partitionKey must not be empty");
		Assert.notNull(builder.payload, "payload must not be null");
		this.streamName = builder.streamName;
		this.partitionKey = builder.partitionKey;
		this.payload = builder.payload;
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
