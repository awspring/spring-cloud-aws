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

import static io.awspring.cloud.sns.core.MessageAttributeDataTypes.NUMBER;
import static io.awspring.cloud.sns.core.MessageAttributeDataTypes.STRING;

import java.util.Map;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * Helper interface used for converting Java types to {@link MessageAttributeValue}.
 * @author Matej Nedić
 */
interface ConvertToMessageAttributes {

	static void populateMapWithStringValue(String attributeCode, @Nullable String value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(STRING).stringValue(value).build());
		}
	}

	static void populateMapWithNumberValue(String attributeCode, @Nullable Number value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(NUMBER).stringValue(value.toString()).build());
		}
	}

	static void populateMapWithNumberValue(String attributeCode, @Nullable String value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(NUMBER).stringValue(value).build());
		}
	}
}
