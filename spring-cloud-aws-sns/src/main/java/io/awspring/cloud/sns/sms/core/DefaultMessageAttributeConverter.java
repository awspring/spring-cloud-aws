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

import static io.awspring.cloud.sns.core.MessageAttributeDataTypes.NUMBER;
import static io.awspring.cloud.sns.core.MessageAttributeDataTypes.STRING;

import io.awspring.cloud.sns.sms.attributes.*;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class DefaultMessageAttributeConverter implements MessageAttributeConverter {

	public Map<String, MessageAttributeValue> convert(SmsMessageAttributes attr) {
		Map<String, MessageAttributeValue> map = new HashMap<>();
		if (attr == null) {
			return map;
		}
		populateMapWithStringValue(AttributeCodes.SENDER_ID, attr.getSenderID(), map);
		populateMapWithStringValue(AttributeCodes.ORIGINATION_NUMBER, attr.getOriginationNumber(), map);
		populateMapWithNumberValue(AttributeCodes.MAX_PRICE, attr.getMaxPrice(), map);
		populateMapWithStringValue(AttributeCodes.SMS_TYPE, attr.getSmsType() != null ? attr.getSmsType().type : null,
				map);
		populateMapWithStringValue(AttributeCodes.ENTITY_ID, attr.getEntityId(), map);
		populateMapWithStringValue(AttributeCodes.TEMPLATE_ID, attr.getTemplateId(), map);
		if (attr.getAdm() != null) {
			populateMapWithNumberValue(AttributeCodes.ADM_TTL, attr.getAdm().getTtl(), map);
		}
		APN apn = attr.getApn();
		if (apn != null) {
			populateMapWithStringValue(AttributeCodes.APN_COLLAPSE_ID, apn.getCollapseId(), map);
			populateMapWithStringValue(AttributeCodes.APN_PRIORITY, apn.getPriority(), map);
			populateMapWithStringValue(AttributeCodes.APN_PUSH_TYPE, apn.getPushType(), map);
			populateMapWithStringValue(AttributeCodes.APN_TOPIC, apn.getTopic(), map);
			populateMapWithNumberValue(AttributeCodes.APN_TTL, apn.getTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_MDM_TTL, apn.getMdmTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_MDM_SANDBOX_TTL, apn.getMdmSandboxTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_PASSBOOK_TTL, apn.getPassbookTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_PASSBOOK_SANDBOX_TTL, apn.getPassbookSandboxTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_VOIP_TTL, apn.getVoipTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_VOIP_SANDBOX_TTL, apn.getVoipSandboxTtl(), map);
			populateMapWithNumberValue(AttributeCodes.APN_SANDBOX_TTL, apn.getSandboxTtl(), map);
		}
		Baidu baidu = attr.getBaidu();
		if (baidu != null) {
			populateMapWithStringValue(AttributeCodes.BAIDU_DEPLOY_STATUS, baidu.getDeployStatus(), map);
			populateMapWithStringValue(AttributeCodes.BAIDU_MESSAGE_KEY, baidu.getMessageKey(), map);
			populateMapWithStringValue(AttributeCodes.BAIDU_MESSAGE_TYPE, baidu.getMessageType(), map);
			populateMapWithNumberValue(AttributeCodes.BAIDU_TTL, baidu.getTtl(), map);
		}
		FCM fcm = attr.getFcm();
		if (fcm != null) {
			populateMapWithNumberValue(AttributeCodes.FCM_TTL, fcm.getFcmTtl(), map);
			populateMapWithNumberValue(AttributeCodes.GCM_TTL, fcm.getGcmTtl(), map);
		}
		MacOS macOS = attr.getMacOS();
		if (macOS != null) {
			populateMapWithNumberValue(AttributeCodes.MACOS_TTL, macOS.getTtl(), map);
			populateMapWithNumberValue(AttributeCodes.MACOS_SANDBOX_TTL, macOS.getSandboxTtl(), map);
		}
		MPNS mpns = attr.getMpns();
		if (mpns != null) {
			populateMapWithNumberValue(AttributeCodes.MPNS_TTL, mpns.getTtl(), map);
			populateMapWithStringValue(AttributeCodes.MPNS_TYPE, mpns.getType(), map);
			populateMapWithStringValue(AttributeCodes.MPNS_NOTIFICATION_CLASS, mpns.getNotificationClass(), map);
		}
		WNS wns = attr.getWns();
		if (wns != null) {
			populateMapWithStringValue(AttributeCodes.WNS_CACHE_POLICY, wns.getCachePolicy(), map);
			populateMapWithStringValue(AttributeCodes.WNS_GROUP, wns.getGroup(), map);
			populateMapWithStringValue(AttributeCodes.WNS_MATCH, wns.getMatch(), map);
			populateMapWithStringValue(AttributeCodes.WNS_TAG, wns.getTag(), map);
			populateMapWithStringValue(AttributeCodes.WNS_TYPE, wns.getType(), map);
			populateMapWithStringValue(AttributeCodes.WNS_SUPPRESS_POPUP, wns.getSuppressPopUp(), map);
			populateMapWithNumberValue(AttributeCodes.WNS_TTL, wns.getTtl(), map);
		}
		return map;
	}

	private void populateMapWithStringValue(String attributeCode, String value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(STRING).stringValue(value).build());
		}
	}

	private void populateMapWithNumberValue(String attributeCode, Number value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(NUMBER).stringValue(value.toString()).build());
		}
	}

	private void populateMapWithNumberValue(String attributeCode, String value,
			Map<String, MessageAttributeValue> messageAttributeValueMap) {
		if (value != null) {
			messageAttributeValueMap.put(attributeCode,
					MessageAttributeValue.builder().dataType(NUMBER).stringValue(value).build());
		}
	}

}
