package io.awspring.cloud.sns.sms.core;

import io.awspring.cloud.sns.core.MessageAttributeDataTypes;
import io.awspring.cloud.sns.sms.attributes.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

import java.util.Map;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SmsMessageAttributesConverter {
	void testUpperLvlFields() {
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().smsType(SmsType.PROMOTIONAL)
			.senderID("Sender_007").originationNumber("09091s").maxPrice("100")
			.entityId("SPRING_CLOUD_AWS").templateId("Template192").build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes
			.convert();
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
			.builder().baidu(Baidu.builder().messageType("newMessage").messageKey("messageKey")
				.ttl(100L).deployStatus("ready").build())
			.adm(ADM.builder().ttl(200L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes
			.convert();

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
			.macOS(MacOS.builder().ttl(200L).sandboxTtl(300L).build()).mpns(MPNS.builder()
				.ttl(400L).type("goodType").notificationClass("I am notified YAY!").build())
			.fcm(FCM.builder().fcmTtl(600L).gcmTtl(700L).build()).build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes
			.convert();

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
			.wns(WNS.builder().cachePolicy("always").group("test").match("matched")
				.tag("tag007").ttl(100L).suppressPopUp("true").type("strong").build())
			.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes
			.convert();

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
		SmsMessageAttributes smsMessageAttributes = SmsMessageAttributes.builder().smsType(SmsType.PROMOTIONAL)
			.senderID("Sender_007").originationNumber("09091s").maxPrice("100")
			.entityId("SPRING_CLOUD_AWS").templateId("Template192")
			.macOS(MacOS.builder().ttl(200L).sandboxTtl(300L).build())
			.baidu(Baidu.builder().messageType("newMessage").messageKey("messageKey").ttl(100L)
				.deployStatus("ready").build())
			.adm(ADM.builder().ttl(200L).build())
			.mpns(MPNS.builder().ttl(400L).type("goodType").notificationClass("I am notified YAY!")
				.build())
			.fcm(FCM.builder().fcmTtl(600L).gcmTtl(700L).build())
			.wns(WNS.builder().cachePolicy("always").group("test").match("matched")
				.tag("tag007").ttl(100L).suppressPopUp("true").type("strong").build())
			.apn(APN.builder().ttl(100L).mdmTtl(200L).mdmSandboxTtl(300L).passbookTtl(400L).passbookSandboxTtl(500L).sandboxTtl(600L).voipTtl(700L).voipSandboxTtl(800L).collapseId("collapsed").priority("high").pushType("always").topic("topic").build())
			.build();
		Map<String, MessageAttributeValue> messageAttributeValueMap = smsMessageAttributes
			.convert();

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

		MessageAttributeValue apnMdmTtl = messageAttributeValueMap.get(AttributeCodes.APN_MDM_TTL);
		Assertions.assertThat(apnMdmTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnMdmTtl.stringValue()).isEqualTo("200");

		MessageAttributeValue apnTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_SANDBOX_TTL);
		Assertions.assertThat(apnTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnTtlSandbox.stringValue()).isEqualTo("600");

		MessageAttributeValue apnMdmTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_MDM_SANDBOX_TTL);
		Assertions.assertThat(apnMdmTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnMdmTtlSandbox.stringValue()).isEqualTo("300");

		MessageAttributeValue apnPassbookTtl = messageAttributeValueMap.get(AttributeCodes.APN_PASSBOOK_TTL);
		Assertions.assertThat(apnPassbookTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnPassbookTtl.stringValue()).isEqualTo("400");

		MessageAttributeValue apnPassbookTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_PASSBOOK_SANDBOX_TTL);
		Assertions.assertThat(apnPassbookTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnPassbookTtlSandbox.stringValue()).isEqualTo("500");

		MessageAttributeValue apnVoipTtl = messageAttributeValueMap.get(AttributeCodes.APN_VOIP_TTL);
		Assertions.assertThat(apnVoipTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnVoipTtl.stringValue()).isEqualTo("700");



		MessageAttributeValue apnTtl = messageAttributeValueMap.get(AttributeCodes.APN_TTL);
		Assertions.assertThat(apnTtl.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnTtl.stringValue()).isEqualTo("100");

		MessageAttributeValue apnVoipTtlSandbox = messageAttributeValueMap.get(AttributeCodes.APN_VOIP_SANDBOX_TTL);
		Assertions.assertThat(apnVoipTtlSandbox.dataType()).isEqualTo(MessageAttributeDataTypes.NUMBER);
		Assertions.assertThat(apnVoipTtlSandbox.stringValue()).isEqualTo("800");


		MessageAttributeValue apnCollapseId = messageAttributeValueMap.get(AttributeCodes.APN_COLLAPSE_ID);
		Assertions.assertThat(apnCollapseId.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(apnCollapseId.stringValue()).isEqualTo("collapsed");

		MessageAttributeValue apnPriority = messageAttributeValueMap.get(AttributeCodes.APN_PRIORITY);
		Assertions.assertThat(apnPriority.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(apnPriority.stringValue()).isEqualTo("high");

		MessageAttributeValue apnPushType = messageAttributeValueMap.get(AttributeCodes.APN_PUSH_TYPE);
		Assertions.assertThat(apnPushType.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(apnPushType.stringValue()).isEqualTo("always");

		MessageAttributeValue apnTopic = messageAttributeValueMap.get(AttributeCodes.APN_TOPIC);
		Assertions.assertThat(apnTopic.dataType()).isEqualTo(MessageAttributeDataTypes.STRING);
		Assertions.assertThat(apnTopic.stringValue()).isEqualTo("topic");

		Assertions.assertThat(messageAttributeValueMap.size()).isEqualTo(37);
	}
}
