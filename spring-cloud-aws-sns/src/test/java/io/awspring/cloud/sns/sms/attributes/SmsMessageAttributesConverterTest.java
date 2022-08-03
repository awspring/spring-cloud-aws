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

	@Test
	void testBaidu_ADM() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().baidu(Baidu.builder()
				.messageType("newMessage").messageKey("messageKey").ttl(100L).deployStatus("ready").build())
				.adm(ADM.builder().ttl(200L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes.convert();

		MessageAttributeValue messageKey = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_KEY);
		assertThat(messageKey.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(messageKey.stringValue()).isEqualTo("messageKey");

		MessageAttributeValue messageType = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_TYPE);
		assertThat(messageType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(messageType.stringValue()).isEqualTo("newMessage");

		MessageAttributeValue deployStatus = messageAttributeValueMap.get(AttributeCodes.BAIDU_DEPLOY_STATUS);
		assertThat(deployStatus.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(deployStatus.stringValue()).isEqualTo("ready");

		MessageAttributeValue baiduTtl = messageAttributeValueMap.get(AttributeCodes.BAIDU_TTL);
		assertThat(baiduTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(baiduTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue admTTL = messageAttributeValueMap.get(AttributeCodes.ADM_TTL);
		assertThat(admTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(admTTL.stringValue()).isEqualTo("200");

		assertThat(messageAttributeValueMap.size()).isEqualTo(5);
	}

	@Test
	void testFCM_MacOS_MPNS() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder()
				.macOS(MacOS.builder().ttl(200L).sandboxTtl(300L).build())
				.mpns(MPNS.builder().ttl(400L).type("goodType").notificationClass("I am notified YAY!").build())
				.fcm(FCM.builder().fcmTtl(600L).gcmTtl(700L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes.convert();

		MessageAttributeValue macOSTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_TTL);
		assertThat(macOSTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(macOSTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue macosSandboxTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_SANDBOX_TTL);
		assertThat(macosSandboxTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(macosSandboxTtl.stringValue()).isEqualTo("300");

		MessageAttributeValue mpnsTtl = messageAttributeValueMap.get(AttributeCodes.MPNS_TTL);
		assertThat(mpnsTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(mpnsTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue mpnsType = messageAttributeValueMap.get(AttributeCodes.MPNS_TYPE);
		assertThat(mpnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(mpnsType.stringValue()).isEqualTo("goodType");

		MessageAttributeValue mpnsNotificationClass = messageAttributeValueMap
				.get(AttributeCodes.MPNS_NOTIFICATION_CLASS);
		assertThat(mpnsNotificationClass.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(mpnsNotificationClass.stringValue()).isEqualTo("I am notified YAY!");

		MessageAttributeValue fcmTtl = messageAttributeValueMap.get(AttributeCodes.FCM_TTL);
		assertThat(fcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(fcmTtl.stringValue()).isEqualTo("600");

		MessageAttributeValue gcmTtl = messageAttributeValueMap.get(AttributeCodes.GCM_TTL);
		assertThat(gcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(gcmTtl.stringValue()).isEqualTo("700");

		assertThat(messageAttributeValueMap.size()).isEqualTo(7);
	}

	@Test
	void testWNS() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder()
				.wns(WNS.builder().cachePolicy("always").group("test").match("matched").tag("tag007").ttl(100L)
						.suppressPopUp("true").type("strong").build())
				.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes.convert();

		MessageAttributeValue wnsTTL = messageAttributeValueMap.get(AttributeCodes.WNS_TTL);
		assertThat(wnsTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(wnsTTL.stringValue()).isEqualTo("100");

		MessageAttributeValue wnsGroup = messageAttributeValueMap.get(AttributeCodes.WNS_GROUP);
		assertThat(wnsGroup.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsGroup.stringValue()).isEqualTo("test");

		MessageAttributeValue wnsCachePolicy = messageAttributeValueMap.get(AttributeCodes.WNS_CACHE_POLICY);
		assertThat(wnsCachePolicy.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsCachePolicy.stringValue()).isEqualTo("always");

		MessageAttributeValue wnsType = messageAttributeValueMap.get(AttributeCodes.WNS_TYPE);
		assertThat(wnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsType.stringValue()).isEqualTo("strong");

		MessageAttributeValue wnsMatch = messageAttributeValueMap.get(AttributeCodes.WNS_MATCH);
		assertThat(wnsMatch.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsMatch.stringValue()).isEqualTo("matched");

		MessageAttributeValue wnsSuppressPopUp = messageAttributeValueMap.get(AttributeCodes.WNS_SUPPRESS_POPUP);
		assertThat(wnsSuppressPopUp.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsSuppressPopUp.stringValue()).isEqualTo("true");

		MessageAttributeValue wnsTag = messageAttributeValueMap.get(AttributeCodes.WNS_TAG);
		assertThat(wnsTag.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsTag.stringValue()).isEqualTo("tag007");

		assertThat(messageAttributeValueMap.size()).isEqualTo(7);
	}

	@Test
	void testAll() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().smsType(SmsType.PROMOTIONAL)
				.senderID("Sender_007").originationNumber("09091s").maxPrice("100").entityId("SPRING_CLOUD_AWS")
				.templateId("Template192").macOS(MacOS.builder().ttl(200L).sandboxTtl(300L).build())
				.baidu(Baidu.builder().messageType("newMessage").messageKey("messageKey").ttl(100L)
						.deployStatus("ready").build())
				.adm(ADM.builder().ttl(200L).build())
				.mpns(MPNS.builder().ttl(400L).type("goodType").notificationClass("I am notified YAY!").build())
				.fcm(FCM.builder().fcmTtl(600L).gcmTtl(700L).build())
				.wns(WNS.builder().cachePolicy("always").group("test").match("matched").tag("tag007").ttl(100L)
						.suppressPopUp("true").type("strong").build())
				.apn(APN.builder().ttl(100L).mdmTtl(200L).mdmSandboxTtl(300L).passbookTtl(400L).passbookSandboxTtl(500L)
						.sandboxTtl(600L).voipTtl(700L).voipSandboxTtl(800L).collapseId("collapsed").priority("high")
						.pushType("always").topic("topic").build())
				.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes.convert();

		MessageAttributeValue wnsTTL = messageAttributeValueMap.get(AttributeCodes.WNS_TTL);
		assertThat(wnsTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(wnsTTL.stringValue()).isEqualTo("100");

		MessageAttributeValue wnsGroup = messageAttributeValueMap.get(AttributeCodes.WNS_GROUP);
		assertThat(wnsGroup.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsGroup.stringValue()).isEqualTo("test");

		MessageAttributeValue wnsCachePolicy = messageAttributeValueMap.get(AttributeCodes.WNS_CACHE_POLICY);
		assertThat(wnsCachePolicy.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsCachePolicy.stringValue()).isEqualTo("always");

		MessageAttributeValue wnsType = messageAttributeValueMap.get(AttributeCodes.WNS_TYPE);
		assertThat(wnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsType.stringValue()).isEqualTo("strong");

		MessageAttributeValue wnsMatch = messageAttributeValueMap.get(AttributeCodes.WNS_MATCH);
		assertThat(wnsMatch.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsMatch.stringValue()).isEqualTo("matched");

		MessageAttributeValue wnsSuppressPopUp = messageAttributeValueMap.get(AttributeCodes.WNS_SUPPRESS_POPUP);
		assertThat(wnsSuppressPopUp.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsSuppressPopUp.stringValue()).isEqualTo("true");

		MessageAttributeValue wnsTag = messageAttributeValueMap.get(AttributeCodes.WNS_TAG);
		assertThat(wnsTag.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(wnsTag.stringValue()).isEqualTo("tag007");

		MessageAttributeValue macOSTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_TTL);
		assertThat(macOSTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(macOSTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue macosSandboxTtl = messageAttributeValueMap.get(AttributeCodes.MACOS_SANDBOX_TTL);
		assertThat(macosSandboxTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(macosSandboxTtl.stringValue()).isEqualTo("300");

		MessageAttributeValue mpnsTtl = messageAttributeValueMap.get(AttributeCodes.MPNS_TTL);
		assertThat(mpnsTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(mpnsTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue mpnsType = messageAttributeValueMap.get(AttributeCodes.MPNS_TYPE);
		assertThat(mpnsType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(mpnsType.stringValue()).isEqualTo("goodType");

		MessageAttributeValue mpnsNotificationClass = messageAttributeValueMap
				.get(AttributeCodes.MPNS_NOTIFICATION_CLASS);
		assertThat(mpnsNotificationClass.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(mpnsNotificationClass.stringValue()).isEqualTo("I am notified YAY!");

		MessageAttributeValue fcmTtl = messageAttributeValueMap.get(AttributeCodes.FCM_TTL);
		assertThat(fcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(fcmTtl.stringValue()).isEqualTo("600");

		MessageAttributeValue gcmTtl = messageAttributeValueMap.get(AttributeCodes.GCM_TTL);
		assertThat(gcmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(gcmTtl.stringValue()).isEqualTo("700");

		MessageAttributeValue messageKey = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_KEY);
		assertThat(messageKey.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(messageKey.stringValue()).isEqualTo("messageKey");

		MessageAttributeValue messageType = messageAttributeValueMap.get(AttributeCodes.BAIDU_MESSAGE_TYPE);
		assertThat(messageType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(messageType.stringValue()).isEqualTo("newMessage");

		MessageAttributeValue deployStatus = messageAttributeValueMap.get(AttributeCodes.BAIDU_DEPLOY_STATUS);
		assertThat(deployStatus.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(deployStatus.stringValue()).isEqualTo("ready");

		MessageAttributeValue baiduTtl = messageAttributeValueMap.get(AttributeCodes.BAIDU_TTL);
		assertThat(baiduTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(baiduTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue admTTL = messageAttributeValueMap.get(AttributeCodes.ADM_TTL);
		assertThat(admTTL.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(admTTL.stringValue()).isEqualTo("200");

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

		MessageAttributeValue apnMdmTtl = messageAttributeValueMap.get(AttributeCodes.APN_MDM_TTL);
		assertThat(apnMdmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnMdmTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue apnTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_SANDBOX_TTL);
		assertThat(apnTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnTtlSandbox.stringValue()).isEqualTo("600");

		MessageAttributeValue apnMdmTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_MDM_SANDBOX_TTL);
		assertThat(apnMdmTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnMdmTtlSandbox.stringValue()).isEqualTo("300");

		MessageAttributeValue apnPassbookTtl = messageAttributeValueMap.get(AttributeCodes.APN_PASSBOOK_TTL);
		assertThat(apnPassbookTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnPassbookTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue apnPassbookTtlSandbox = messageAttributeValueMap
				.get(AttributeCodes.APN_PASSBOOK_SANDBOX_TTL);
		assertThat(apnPassbookTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnPassbookTtlSandbox.stringValue()).isEqualTo("500");

		MessageAttributeValue apnVoipTtl = messageAttributeValueMap.get(AttributeCodes.APN_VOIP_TTL);
		assertThat(apnVoipTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnVoipTtl.stringValue()).isEqualTo("700");

		MessageAttributeValue apnTtl = messageAttributeValueMap.get(AttributeCodes.APN_TTL);
		assertThat(apnTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue apnVoipTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_VOIP_SANDBOX_TTL);
		assertThat(apnVoipTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		assertThat(apnVoipTtlSandbox.stringValue()).isEqualTo("800");

		MessageAttributeValue apnCollapseId = messageAttributeValueMap.get(AttributeCodes.APN_COLLAPSE_ID);
		assertThat(apnCollapseId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(apnCollapseId.stringValue()).isEqualTo("collapsed");

		MessageAttributeValue apnPriority = messageAttributeValueMap.get(AttributeCodes.APN_PRIORITY);
		assertThat(apnPriority.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(apnPriority.stringValue()).isEqualTo("high");

		MessageAttributeValue apnPushType = messageAttributeValueMap.get(AttributeCodes.APN_PUSH_TYPE);
		assertThat(apnPushType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(apnPushType.stringValue()).isEqualTo("always");

		MessageAttributeValue apnTopic = messageAttributeValueMap.get(AttributeCodes.APN_TOPIC);
		assertThat(apnTopic.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		assertThat(apnTopic.stringValue()).isEqualTo("topic");

		assertThat(messageAttributeValueMap.size()).isEqualTo(37);
	}
}
