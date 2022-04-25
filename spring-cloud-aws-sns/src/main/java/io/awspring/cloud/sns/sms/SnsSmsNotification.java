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
package io.awspring.cloud.sns.sms;

import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SENDER_ID;
import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SMS_TYPE;

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

public class SnsSmsNotification<T> {
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
	public static <T> SnsSmsNotification<T> of(T payload) {
		return new SnsSmsNotification<>(payload, null);
	}

	/**
	 * Creates a builder with payload.
	 *
	 * @param payload - notification payload
	 * @param <T> - type of payload
	 * @return the notification
	 */
	public static <T> SnsSmsNotification.Builder<T> builder(T payload) {
		return new SnsSmsNotification.Builder<T>(payload);
	}

	public SnsSmsNotification(T payload, @Nullable Map<String, Object> headers) {
		this.payload = payload;
		this.headers = headers;
	}

	public T getPayload() {
		return payload;
	}

	@Nullable
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Nullable
	public String getSmsType() {
		Map<String, Object> headersMap = this.headers;
		if (headersMap != null) {
			return (String) headersMap.get(DEFAULT_SMS_TYPE);
		}
		else {
			return null;
		}
	}

	@Nullable
	public String getSenderID() {
		Map<String, Object> headersMap = this.headers;
		if (headersMap != null) {
			return (String) headersMap.get(DEFAULT_SENDER_ID);
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

		public SnsSmsNotification.Builder<T> headers(Map<String, Object> headers) {
			this.headers.putAll(headers);
			return this;
		}

		public SnsSmsNotification.Builder<T> header(String key, Object value) {
			this.headers.put(key, value);
			return this;
		}

		public SnsSmsNotification.Builder<T> senderId(String senderId) {
			this.headers.put(DEFAULT_SENDER_ID, senderId);
			return this;
		}

		public SnsSmsNotification.Builder<T> smsType(String smsType) {
			this.headers.put(DEFAULT_SMS_TYPE, smsType);
			return this;
		}

		public SnsSmsNotification<T> build() {
			return new SnsSmsNotification<T>(payload, headers);
		}
	}
}
