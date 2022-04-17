/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.core;

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * SNS notification object.
 * @param <T> - type of payload
 */
public class SnsNotification<T> {

	private final T payload;

	@Nullable
	private final Map<String, Object> headers;

	/**
	 * Creates notification from payload.
	 *
	 * @param payload - notification payload
	 * @param <T> - type of payload
	 * @return the notification
	 */
	public static <T> SnsNotification<T> of(T payload) {
		return new SnsNotification<>(payload, null);
	}

	/**
	 * Creates a builder with payload.
	 *
	 * @param payload - notification payload
	 * @param <T> - type of payload
	 * @return the notification
	 */
	public static <T> Builder<T> builder(T payload) {
		return new Builder<T>(payload);
	}

	public SnsNotification(T payload, @Nullable Map<String, Object> headers) {
		this.payload = payload;
		this.headers = headers;
	}

	@Nullable
	public String getSubject() {
		Map<String, Object> headersMap = this.headers;
		if (headersMap != null) {
			return (String) headersMap.get(SnsHeaders.NOTIFICATION_SUBJECT_HEADER);
		}
		else {
			return null;
		}
	}

	public T getPayload() {
		return payload;
	}

	@Nullable
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Nullable
	public String getGroupId() {
		Map<String, Object> headersMap = this.headers;
		if (headersMap != null) {
			return (String) headersMap.get(SnsHeaders.MESSAGE_GROUP_ID_HEADER);
		}
		else {
			return null;
		}
	}

	@Nullable
	public String getDeduplicationId() {
		Map<String, Object> headersMap = this.headers;
		if (headersMap != null) {
			return (String) headersMap.get(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER);
		}
		else {
			return null;
		}
	}

	public static class Builder<T> {
		private final T payload;
		private final Map<String, Object> headers = new HashMap<>();

		Builder(T payload) {
			this.payload = payload;
		}

		public Builder<T> subject(String subject) {
			this.headers.put(SnsHeaders.NOTIFICATION_SUBJECT_HEADER, subject);
			return this;
		}

		public Builder<T> headers(Map<String, Object> headers) {
			this.headers.putAll(headers);
			return this;
		}

		public Builder<T> header(String key, Object value) {
			this.headers.put(key, value);
			return this;
		}

		public Builder<T> groupId(String groupId) {
			this.headers.put(SnsHeaders.MESSAGE_GROUP_ID_HEADER, groupId);
			return this;
		}

		public Builder<T> deduplicationId(String deduplicationId) {
			this.headers.put(SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER, deduplicationId);
			return this;
		}

		public SnsNotification<T> build() {
			return new SnsNotification<T>(payload, headers);
		}
	}
}
