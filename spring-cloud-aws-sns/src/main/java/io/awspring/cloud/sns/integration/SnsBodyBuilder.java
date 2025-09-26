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
package io.awspring.cloud.sns.integration;

import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * A utility class to simplify an SNS Message body building. Can be used from the
 * {@code SnsMessageHandler#bodyExpression} definition or directly in case of manual
 * {@link software.amazon.awssdk.services.sns.model.PublishRequest} building.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public final class SnsBodyBuilder {

	private final Map<String, String> snsMessage = new HashMap<>();

	private SnsBodyBuilder(String defaultMessage) {
		Assert.hasText(defaultMessage, "defaultMessage must not be empty.");
		this.snsMessage.put("default", defaultMessage);
	}

	public SnsBodyBuilder forProtocols(String message, String... protocols) {
		Assert.hasText(message, "message must not be empty.");
		Assert.notEmpty(protocols, "protocols must not be empty.");
		for (String protocol : protocols) {
			Assert.hasText(protocol, "protocols must not contain empty elements.");
			this.snsMessage.put(protocol, message);
		}
		return this;
	}

	public String build() {
		StringBuilder stringBuilder = new StringBuilder("{");
		for (Map.Entry<String, String> entry : this.snsMessage.entrySet()) {
			stringBuilder.append("\"").append(entry.getKey()).append("\":\"")
					.append(entry.getValue().replaceAll("\"", "\\\\\"")).append("\",");
		}
		return stringBuilder.substring(0, stringBuilder.length() - 1) + "}";
	}

	public static SnsBodyBuilder withDefault(String defaultMessage) {
		return new SnsBodyBuilder(defaultMessage);
	}

}
