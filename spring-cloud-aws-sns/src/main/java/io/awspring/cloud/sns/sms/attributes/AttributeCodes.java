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

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class AttributeCodes {

	private AttributeCodes() {
	}

	public static final String SENDER_ID = "AWS.SNS.SMS.SenderID";
	public static final String ORIGINATION_NUMBER = "AWS.MM.SMS.OriginationNumber";
	public static final String MAX_PRICE = "AWS.SNS.SMS.MaxPrice";
	public static final String SMS_TYPE = "AWS.SNS.SMS.SMSType";
	public static final String ENTITY_ID = "AWS.MM.SMS.EntityId";
	public static final String TEMPLATE_ID = "AWS.MM.SMS.TemplateId";
	public static final String ADM_TTL = "AWS.SNS.MOBILE.ADM.TTL";
	public static final String APN_MDM_TTL = "AWS.SNS.MOBILE.APNS_MDM.TTL";
	public static final String APN_MDM_SANDBOX_TTL = "AWS.SNS.MOBILE.APNS_MDM_SANDBOX.TTL";
	public static final String APN_PASSBOOK_TTL = "AWS.SNS.MOBILE.APNS_PASSBOOK.TTL";
	public static final String APN_PASSBOOK_SANDBOX_TTL = "AWS.SNS.MOBILE.APNS_PASSBOOK_SANDBOX.TTL";
	public static final String APN_SANDBOX_TTL = "AWS.SNS.MOBILE.APNS_SANDBOX.TTL";
	public static final String APN_VOIP_TTL = "AWS.SNS.MOBILE.APNS_VOIP.TTL";
	public static final String APN_VOIP_SANDBOX_TTL = "AWS.SNS.MOBILE.APNS_VOIP_SANDBOX.TTL";
	public static final String APN_COLLAPSE_ID = "AWS.SNS.MOBILE.APNS.COLLAPSE_ID";
	public static final String APN_PRIORITY = "AWS.SNS.MOBILE.APNS.PRIORITY";
	public static final String APN_PUSH_TYPE = "AWS.SNS.MOBILE.APNS.PUSH_TYPE";
	public static final String APN_TOPIC = "AWS.SNS.MOBILE.APNS.TOPIC";
	public static final String APN_TTL = "AWS.SNS.MOBILE.APNS.TTL";
	public static final String BAIDU_DEPLOY_STATUS = "AWS.SNS.MOBILE.BAIDU.DeployStatus";
	public static final String BAIDU_MESSAGE_KEY = "AWS.SNS.MOBILE.BAIDU.MessageKey";
	public static final String BAIDU_MESSAGE_TYPE = "AWS.SNS.MOBILE.BAIDU.MessageType";
	public static final String BAIDU_TTL = "AWS.SNS.MOBILE.BAIDU.TTL";
	public static final String FCM_TTL = "AWS.SNS.MOBILE.FCM.TTL";
	public static final String GCM_TTL = "AWS.SNS.MOBILE.GCM.TTL";
	public static final String MACOS_SANDBOX_TTL = "AWS.SNS.MOBILE.MACOS_SANDBOX.TTL";
	public static final String MACOS_TTL = "AWS.SNS.MOBILE.MACOS.TTL";
	public static final String MPNS_NOTIFICATION_CLASS = "AWS.SNS.MOBILE.MPNS.NotificationClass";
	public static final String MPNS_TTL = "AWS.SNS.MOBILE.MPNS.TTL";
	public static final String MPNS_TYPE = "AWS.SNS.MOBILE.MPNS.Type";
	public static final String WNS_CACHE_POLICY = "AWS.SNS.MOBILE.WNS.CachePolicy";
	public static final String WNS_GROUP = "AWS.SNS.MOBILE.WNS.Group";
	public static final String WNS_MATCH = "AWS.SNS.MOBILE.WNS.Match";
	public static final String WNS_SUPPRESS_POPUP = "AWS.SNS.MOBILE.WNS.SuppressPopup";
	public static final String WNS_TAG = "AWS.SNS.MOBILE.WNS.Tag";
	public static final String WNS_TTL = "AWS.SNS.MOBILE.WNS.TTL";
	public static final String WNS_TYPE = "AWS.SNS.MOBILE.WNS.Type";

}
