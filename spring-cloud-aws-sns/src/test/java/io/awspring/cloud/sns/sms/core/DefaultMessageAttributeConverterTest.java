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
package io.awspring.cloud.sns.sms.core;

import io.awspring.cloud.sns.core.MessageAttributeDataTypes;
import io.awspring.cloud.sns.sms.attributes.*;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class DefaultMessageAttributeConverterTest {

	DefaultMessageAttributeConverter defaultMessageAttributeConverterTest = new DefaultMessageAttributeConverter();

	@Test
	void testUpperLvlFields() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().withSmsType(SmsType.PROMOTIONAL)
				.withSenderID("Sender_007").withOriginationNumber("09091s").withMaxPrice("100")
				.withEntityId("SPRING_CLOUD_AWS").withTemplateId("Template192").build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = defaultMessageAttributeConverterTest
				.convert(smsMessageAttributes);
		MessageAttributeValue smsType = messageAttributeValueMap.get(AttributeCodes.SMS_TYPE);
		Assertions.assertThat(smsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(smsType.stringValue()).isEqualTo(SmsType.PROMOTIONAL.type);

		MessageAttributeValue orgNumber = messageAttributeValueMap.get(AttributeCodes.ORIGINATION_NUMBER);
		Assertions.assertThat(orgNumber.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(orgNumber.stringValue()).isEqualTo("09091s");

		MessageAttributeValue senderId = messageAttributeValueMap.get(AttributeCodes.SENDER_ID);
		Assertions.assertThat(senderId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(senderId.stringValue()).isEqualTo("Sender_007");

		MessageAttributeValue maxPrice = messageAttributeValueMap.get(AttributeCodes.MAX_PRICE);
		Assertions.assertThat(maxPrice.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(maxPrice.stringValue()).isEqualTo("100");

		MessageAttributeValue entityId = messageAttributeValueMap.get(AttributeCodes.ENTITY_ID);
		Assertions.assertThat(entityId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(entityId.stringValue()).isEqualTo("SPRING_CLOUD_AWS");

		MessageAttributeValue templateId = messageAttributeValueMap.get(AttributeCodes.TEMPLATE_ID);
		Assertions.assertThat(templateId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(templateId.stringValue()).isEqualTo("Template192");
		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(6);
	}

	@Test
	void testBaidu_ADM() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes
				.builder().withBaidu(Baidu.builder().withMessageType("newMessage").withMessageKey("messageKey")
						.withTtl(100L).withDeployStatus("ready").build())
				.withAdm(ADM.builder().withTtl(200L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = defaultMessageAttributeConverterTest
				.convert(smsMessageAttributes);

		MessageAttributeValue messageKey = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_KEY);
		Assertions.assertThat(messageKey.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(messageKey.stringValue()).isEqualTo("messageKey");

		MessageAttributeValue messageType = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_TYPE);
		Assertions.assertThat(messageType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(messageType.stringValue()).isEqualTo("newMessage");

		MessageAttributeValue deployStatus = messageAttributeValueMap.get(AttributeCodes.BAIDU_DEPLOY_STATUS);
		Assertions.assertThat(deployStatus.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(deployStatus.stringValue()).isEqualTo("ready");

		MessageAttributeValue baiduTtl = messageAttributeValueMap.get(AttributeCodes.BAIDU_TTL);
		Assertions.assertThat(baiduTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(baiduTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue admTTL = messageAttributeValueMap.get(AttributeCodes.ADM_TTL);
		Assertions.assertThat(admTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(admTTL.stringValue()).isEqualTo("200");

		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(5);
	}

	@Test
	void testFCM_MacOS_MPNS() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder()
				.withMacOS(MacOS.builder().withTtl(200L).withSandboxTtl(300L).build()).withMpns(MPNS.builder()
						.withTtl(400L).withType("goodType").withNotificationClass("I am notified YAY!").build())
				.withFcm(FCM.builder().withFcmTtl(600L).withGcmTtl(700L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = defaultMessageAttributeConverterTest
				.convert(smsMessageAttributes);

		MessageAttributeValue macOSTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_TTL);
		Assertions.assertThat(macOSTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(macOSTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue macosSandboxTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_SANDBOX_TTL);
		Assertions.assertThat(macosSandboxTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(macosSandboxTtl.stringValue()).isEqualTo("300");

		MessageAttributeValue mpnsTtl = messageAttributeValueMap.get(AttributeCodes.MPNS_TTL);
		Assertions.assertThat(mpnsTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(mpnsTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue mpnsType = messageAttributeValueMap.get(AttributeCodes.MPNS_TYPE);
		Assertions.assertThat(mpnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(mpnsType.stringValue()).isEqualTo("goodType");

		MessageAttributeValue mpnsNotificationClass = messageAttributeValueMap
				.get(AttributeCodes.MPNS_NOTIFICATION_CLASS);
		Assertions.assertThat(mpnsNotificationClass.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(mpnsNotificationClass.stringValue()).isEqualTo("I am notified YAY!");

		MessageAttributeValue fcmTtl = messageAttributeValueMap.get(AttributeCodes.FCM_TTL);
		Assertions.assertThat(fcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(fcmTtl.stringValue()).isEqualTo("600");

		MessageAttributeValue gcmTtl = messageAttributeValueMap.get(AttributeCodes.GCM_TTL);
		Assertions.assertThat(gcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(gcmTtl.stringValue()).isEqualTo("700");

		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(7);
	}

	@Test
	void testWNS() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder()
				.withWns(WNS.builder().withCachePolicy("always").withGroup("test").withMatch("matched")
						.withTag("tag007").withTtl(100L).withSuppressPopUp("true").withType("strong").build())
				.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = defaultMessageAttributeConverterTest
				.convert(smsMessageAttributes);

		MessageAttributeValue wnsTTL = messageAttributeValueMap.get(AttributeCodes.WNS_TTL);
		Assertions.assertThat(wnsTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(wnsTTL.stringValue()).isEqualTo("100");

		MessageAttributeValue wnsGroup = messageAttributeValueMap.get(AttributeCodes.WNS_GROUP);
		Assertions.assertThat(wnsGroup.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsGroup.stringValue()).isEqualTo("test");

		MessageAttributeValue wnsCachePolicy = messageAttributeValueMap.get(AttributeCodes.WNS_CACHE_POLICY);
		Assertions.assertThat(wnsCachePolicy.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsCachePolicy.stringValue()).isEqualTo("always");

		MessageAttributeValue wnsType = messageAttributeValueMap.get(AttributeCodes.WNS_TYPE);
		Assertions.assertThat(wnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsType.stringValue()).isEqualTo("strong");

		MessageAttributeValue wnsMatch = messageAttributeValueMap.get(AttributeCodes.WNS_MATCH);
		Assertions.assertThat(wnsMatch.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsMatch.stringValue()).isEqualTo("matched");

		MessageAttributeValue wnsSuppressPopUp = messageAttributeValueMap.get(AttributeCodes.WNS_SUPPRESS_POPUP);
		Assertions.assertThat(wnsSuppressPopUp.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsSuppressPopUp.stringValue()).isEqualTo("true");

		MessageAttributeValue wnsTag = messageAttributeValueMap.get(AttributeCodes.WNS_TAG);
		Assertions.assertThat(wnsTag.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsTag.stringValue()).isEqualTo("tag007");

		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(7);
	}

	@Test
	void testAll() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().withSmsType(SmsType.PROMOTIONAL)
				.withSenderID("Sender_007").withOriginationNumber("09091s").withMaxPrice("100")
				.withEntityId("SPRING_CLOUD_AWS").withTemplateId("Template192")
				.withMacOS(MacOS.builder().withTtl(200L).withSandboxTtl(300L).build())
				.withBaidu(Baidu.builder().withMessageType("newMessage").withMessageKey("messageKey").withTtl(100L)
						.withDeployStatus("ready").build())
				.withAdm(ADM.builder().withTtl(200L).build())
				.withMpns(MPNS.builder().withTtl(400L).withType("goodType").withNotificationClass("I am notified YAY!")
						.build())
				.withFcm(FCM.builder().withFcmTtl(600L).withGcmTtl(700L).build())
				.withWns(WNS.builder().withCachePolicy("always").withGroup("test").withMatch("matched")
						.withTag("tag007").withTtl(100L).withSuppressPopUp("true").withType("strong").build())
				.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = defaultMessageAttributeConverterTest
				.convert(smsMessageAttributes);

		MessageAttributeValue wnsTTL = messageAttributeValueMap.get(AttributeCodes.WNS_TTL);
		Assertions.assertThat(wnsTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(wnsTTL.stringValue()).isEqualTo("100");

		MessageAttributeValue wnsGroup = messageAttributeValueMap.get(AttributeCodes.WNS_GROUP);
		Assertions.assertThat(wnsGroup.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsGroup.stringValue()).isEqualTo("test");

		MessageAttributeValue wnsCachePolicy = messageAttributeValueMap.get(AttributeCodes.WNS_CACHE_POLICY);
		Assertions.assertThat(wnsCachePolicy.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsCachePolicy.stringValue()).isEqualTo("always");

		MessageAttributeValue wnsType = messageAttributeValueMap.get(AttributeCodes.WNS_TYPE);
		Assertions.assertThat(wnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsType.stringValue()).isEqualTo("strong");

		MessageAttributeValue wnsMatch = messageAttributeValueMap.get(AttributeCodes.WNS_MATCH);
		Assertions.assertThat(wnsMatch.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsMatch.stringValue()).isEqualTo("matched");

		MessageAttributeValue wnsSuppressPopUp = messageAttributeValueMap.get(AttributeCodes.WNS_SUPPRESS_POPUP);
		Assertions.assertThat(wnsSuppressPopUp.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsSuppressPopUp.stringValue()).isEqualTo("true");

		MessageAttributeValue wnsTag = messageAttributeValueMap.get(AttributeCodes.WNS_TAG);
		Assertions.assertThat(wnsTag.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(wnsTag.stringValue()).isEqualTo("tag007");

		MessageAttributeValue macOSTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_TTL);
		Assertions.assertThat(macOSTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(macOSTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue macosSandboxTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_SANDBOX_TTL);
		Assertions.assertThat(macosSandboxTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(macosSandboxTtl.stringValue()).isEqualTo("300");

		MessageAttributeValue mpnsTtl = messageAttributeValueMap.get(AttributeCodes.MPNS_TTL);
		Assertions.assertThat(mpnsTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(mpnsTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue mpnsType = messageAttributeValueMap.get(AttributeCodes.MPNS_TYPE);
		Assertions.assertThat(mpnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(mpnsType.stringValue()).isEqualTo("goodType");

		MessageAttributeValue mpnsNotificationClass = messageAttributeValueMap
				.get(AttributeCodes.MPNS_NOTIFICATION_CLASS);
		Assertions.assertThat(mpnsNotificationClass.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(mpnsNotificationClass.stringValue()).isEqualTo("I am notified YAY!");

		MessageAttributeValue fcmTtl = messageAttributeValueMap.get(AttributeCodes.FCM_TTL);
		Assertions.assertThat(fcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(fcmTtl.stringValue()).isEqualTo("600");

		MessageAttributeValue gcmTtl = messageAttributeValueMap.get(AttributeCodes.GCM_TTL);
		Assertions.assertThat(gcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(gcmTtl.stringValue()).isEqualTo("700");

		MessageAttributeValue messageKey = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_KEY);
		Assertions.assertThat(messageKey.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(messageKey.stringValue()).isEqualTo("messageKey");

		MessageAttributeValue messageType = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_TYPE);
		Assertions.assertThat(messageType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(messageType.stringValue()).isEqualTo("newMessage");

		MessageAttributeValue deployStatus = messageAttributeValueMap.get(AttributeCodes.BAIDU_DEPLOY_STATUS);
		Assertions.assertThat(deployStatus.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(deployStatus.stringValue()).isEqualTo("ready");

		MessageAttributeValue baiduTtl = messageAttributeValueMap.get(AttributeCodes.BAIDU_TTL);
		Assertions.assertThat(baiduTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(baiduTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue admTTL = messageAttributeValueMap.get(AttributeCodes.ADM_TTL);
		Assertions.assertThat(admTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(admTTL.stringValue()).isEqualTo("200");

		MessageAttributeValue smsType = messageAttributeValueMap.get(AttributeCodes.SMS_TYPE);
		Assertions.assertThat(smsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(smsType.stringValue()).isEqualTo(SmsType.PROMOTIONAL.type);

		MessageAttributeValue orgNumber = messageAttributeValueMap.get(AttributeCodes.ORIGINATION_NUMBER);
		Assertions.assertThat(orgNumber.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(orgNumber.stringValue()).isEqualTo("09091s");

		MessageAttributeValue senderId = messageAttributeValueMap.get(AttributeCodes.SENDER_ID);
		Assertions.assertThat(senderId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(senderId.stringValue()).isEqualTo("Sender_007");

		MessageAttributeValue maxPrice = messageAttributeValueMap.get(AttributeCodes.MAX_PRICE);
		Assertions.assertThat(maxPrice.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(maxPrice.stringValue()).isEqualTo("100");

		MessageAttributeValue entityId = messageAttributeValueMap.get(AttributeCodes.ENTITY_ID);
		Assertions.assertThat(entityId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(entityId.stringValue()).isEqualTo("SPRING_CLOUD_AWS");

		MessageAttributeValue templateId = messageAttributeValueMap.get(AttributeCodes.TEMPLATE_ID);
		Assertions.assertThat(templateId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(templateId.stringValue()).isEqualTo("Template192");

		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(25);
	}

}
