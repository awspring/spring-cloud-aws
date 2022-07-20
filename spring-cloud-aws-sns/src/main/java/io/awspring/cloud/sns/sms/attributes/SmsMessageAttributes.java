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

import static io.awspring.cloud.sns.sms.attributes.ConvertToMessageAttributes.populateMapWithNumberValue;
import static io.awspring.cloud.sns.sms.attributes.ConvertToMessageAttributes.populateMapWithStringValue;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SmsMessageAttributes {
	private String senderID;
	private String originationNumber;
	private String maxPrice;
	private SmsType smsType;
	private String entityId;
	private String templateId;
	private String messageGroupId;
	private String deduplicationId;
	private String messageStructure;
	private ADM adm;
	private APN apn;
	private Baidu baidu;
	private FCM fcm;
	private MacOS macOS;
	private MPNS mpns;
	private WNS wns;

	public Map<String, MessageAttributeValue> convert() {
		Map<String, MessageAttributeValue> map = new HashMap<>();
		populateMapWithStringValue(AttributeCodes.SENDER_ID, this.getSenderID(), map);
		populateMapWithStringValue(AttributeCodes.ORIGINATION_NUMBER, this.getOriginationNumber(), map);
		populateMapWithNumberValue(AttributeCodes.MAX_PRICE, this.getMaxPrice(), map);
		populateMapWithStringValue(AttributeCodes.SMS_TYPE, this.getSmsType() != null ? this.getSmsType().type : null,
				map);
		populateMapWithStringValue(AttributeCodes.ENTITY_ID, this.getEntityId(), map);
		populateMapWithStringValue(AttributeCodes.TEMPLATE_ID, this.getTemplateId(), map);
		if (this.getAdm() != null) {
			this.getAdm().convertAndPopulate(map);
		}
		if (this.getApn() != null) {
			this.getApn().convertAndPopulate(map);
		}
		if (this.getBaidu() != null) {
			this.getBaidu().convertAndPopulate(map);
		}
		if (this.getFcm() != null) {
			this.getFcm().convertAndPopulate(map);
		}
		if (this.getMacOS() != null) {
			this.getMacOS().convertAndPopulate(map);
		}
		if (this.getMpns() != null) {
			this.getMpns().convertAndPopulate(map);
		}
		if (this.getWns() != null) {
			this.getWns().convertAndPopulate(map);
		}
		return map;
	}

	public String getSenderID() {
		return senderID;
	}

	public void setSenderID(String senderID) {
		this.senderID = senderID;
	}

	public String getOriginationNumber() {
		return originationNumber;
	}

	public void setOriginationNumber(String originationNumber) {
		this.originationNumber = originationNumber;
	}

	public String getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(String maxPrice) {
		this.maxPrice = maxPrice;
	}

	public SmsType getSmsType() {
		return smsType;
	}

	public void setSmsType(SmsType smsType) {
		this.smsType = smsType;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public void setMessageGroupId(String messageGroupId) {
		this.messageGroupId = messageGroupId;
	}

	public void setDeduplicationId(String deduplicationId) {
		this.deduplicationId = deduplicationId;
	}

	public void setMessageStructure(String messageStructure) {
		this.messageStructure = messageStructure;
	}

	public void setAdm(ADM adm) {
		this.adm = adm;
	}

	public void setApn(APN apn) {
		this.apn = apn;
	}

	public void setBaidu(Baidu baidu) {
		this.baidu = baidu;
	}

	public void setFcm(FCM fcm) {
		this.fcm = fcm;
	}

	public void setMacOS(MacOS macOS) {
		this.macOS = macOS;
	}

	public void setMpns(MPNS mpns) {
		this.mpns = mpns;
	}

	public void setWns(WNS wns) {
		this.wns = wns;
	}

	public String getMessageGroupId() {
		return messageGroupId;
	}

	public String getDeduplicationId() {
		return deduplicationId;
	}

	public String getMessageStructure() {
		return messageStructure;
	}

	public ADM getAdm() {
		return adm;
	}

	public APN getApn() {
		return apn;
	}

	public Baidu getBaidu() {
		return baidu;
	}

	public FCM getFcm() {
		return fcm;
	}

	public MacOS getMacOS() {
		return macOS;
	}

	public MPNS getMpns() {
		return mpns;
	}

	public WNS getWns() {
		return wns;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String senderID;
		private String originationNumber;
		private String maxPrice;
		private SmsType smsType;
		private String entityId;
		private String templateId;
		private String messageGroupId;
		private String deduplicationId;
		private String messageStructure;
		private ADM adm;
		private APN apn;
		private Baidu baidu;
		private FCM fcm;
		private MacOS macOS;
		private MPNS mpns;
		private WNS wns;

		private Builder() {
		}

		public static Builder aSmsMessageAttributes() {
			return new Builder();
		}

		public Builder senderID(String senderID) {
			this.senderID = senderID;
			return this;
		}

		public Builder originationNumber(String originationNumber) {
			this.originationNumber = originationNumber;
			return this;
		}

		public Builder maxPrice(String maxPrice) {
			this.maxPrice = maxPrice;
			return this;
		}

		public Builder smsType(SmsType smsType) {
			this.smsType = smsType;
			return this;
		}

		public Builder entityId(String entityId) {
			this.entityId = entityId;
			return this;
		}

		public Builder templateId(String templateId) {
			this.templateId = templateId;
			return this;
		}

		public Builder messageGroupId(String messageGroupId) {
			this.messageGroupId = messageGroupId;
			return this;
		}

		public Builder deduplicationId(String deduplicationId) {
			this.deduplicationId = deduplicationId;
			return this;
		}

		public Builder messageStructure(String messageStructure) {
			this.messageStructure = messageStructure;
			return this;
		}

		public Builder adm(ADM adm) {
			this.adm = adm;
			return this;
		}

		public Builder apn(APN apn) {
			this.apn = apn;
			return this;
		}

		public Builder baidu(Baidu baidu) {
			this.baidu = baidu;
			return this;
		}

		public Builder fcm(FCM fcm) {
			this.fcm = fcm;
			return this;
		}

		public Builder macOS(MacOS macOS) {
			this.macOS = macOS;
			return this;
		}

		public Builder mpns(MPNS mpns) {
			this.mpns = mpns;
			return this;
		}

		public Builder wns(WNS wns) {
			this.wns = wns;
			return this;
		}

		public SmsMessageAttributes build() {
			SmsMessageAttributes smsMessageAttributes = new SmsMessageAttributes();
			smsMessageAttributes.setSenderID(senderID);
			smsMessageAttributes.setOriginationNumber(originationNumber);
			smsMessageAttributes.setMaxPrice(maxPrice);
			smsMessageAttributes.setSmsType(smsType);
			smsMessageAttributes.setEntityId(entityId);
			smsMessageAttributes.setTemplateId(templateId);
			smsMessageAttributes.setMessageGroupId(messageGroupId);
			smsMessageAttributes.setDeduplicationId(deduplicationId);
			smsMessageAttributes.setMessageStructure(messageStructure);
			smsMessageAttributes.setAdm(adm);
			smsMessageAttributes.setApn(apn);
			smsMessageAttributes.setBaidu(baidu);
			smsMessageAttributes.setFcm(fcm);
			smsMessageAttributes.setMacOS(macOS);
			smsMessageAttributes.setMpns(mpns);
			smsMessageAttributes.setWns(wns);
			return smsMessageAttributes;
		}
	}
}
