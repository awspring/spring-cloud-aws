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
package io.awspring.cloud.sns.sms.attributes;

import static org.assertj.core.api.Assertions.*;

import io.awspring.cloud.sns.core.MessageAttributeDataTypes;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
class SmsMessageAttributesConverterTest {

	@Test
	void testUpperLvlFields() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().smsType(SmsType.PROMOTIONAL)
				.senderID("Sender_007").originationNumber("09091s").maxPrice("100").entityId("SPRING_CLOUD_AWS")
				.templateId("Template192").build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes.convert();
		MessageAttributeValue smsType = messageAttributeValueMap.get(AttributeCodes.SMS_TYPE);
		assertThat(smsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(smsType.stringValue()).isEqualTo(SmsType.PROMOTIONAL.type);

		MessageAttributeValue orgNumber = messageAttributeValueMap.get(AttributeCodes.ORIGINATION_NUMBER);
		assertThat(orgNumber.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(orgNumber.stringValue()).isEqualTo("09091s");

		MessageAttributeValue senderId = messageAttributeValueMap.get(AttributeCodes.SENDER_ID);
		assertThat(senderId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(senderId.stringValue()).isEqualTo("Sender_007");

		MessageAttributeValue maxPrice = messageAttributeValueMap.get(AttributeCodes.MAX_PRICE);
		assertThat(maxPrice.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(maxPrice.stringValue()).isEqualTo("100");

		MessageAttributeValue entityId = messageAttributeValueMap.get(AttributeCodes.ENTITY_ID);
		assertThat(entityId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(entityId.stringValue()).isEqualTo("SPRING_CLOUD_AWS");

		MessageAttributeValue templateId = messageAttributeValueMap.get(AttributeCodes.TEMPLATE_ID);
		assertThat(templateId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(templateId.stringValue()).isEqualTo("Template192");
		assertThat(messageAttributeValueMap.size()).isEqualTo(6);
	}

}
