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
package io.awspring.cloud.sqs.support.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MessageConversionException;

/**
 * @author Michael Sosa
 * @author Alexander Nebel
 * @since 3.3.1
 */
public class SnsJsonNode {
	private final String jsonString;
	private final JsonNode jsonNode;

	public SnsJsonNode(ObjectMapper jsonMapper, String jsonString) {
		try {
			this.jsonString = jsonString;
			jsonNode = jsonMapper.readTree(jsonString);
		}
		catch (Exception e) {
			throw new MessageConversionException("Could not read JSON", e);
		}
		validate();
	}

	void validate() throws MessageConversionException {
		if (!jsonNode.has("Type")) {
			throw new MessageConversionException("Payload: '" + jsonString + "' does not contain a Type attribute",
					null);
		}

		if (!"Notification".equals(jsonNode.get("Type").asText())) {
			throw new MessageConversionException("Payload: '" + jsonString + "' is not a valid notification", null);
		}

		if (!jsonNode.has("Message")) {
			throw new MessageConversionException("Payload: '" + jsonString + "' does not contain a message", null);
		}
	}

	public String getMessageAsString() {
		return jsonNode.get("Message").asText();
	}

	public String getSubjectAsString() {
		if (!jsonNode.has("Subject")) {
			throw new MessageConversionException("Payload: '" + jsonString + "' does not contain a subject", null);
		}
		return jsonNode.get("Subject").asText();
	}
}
