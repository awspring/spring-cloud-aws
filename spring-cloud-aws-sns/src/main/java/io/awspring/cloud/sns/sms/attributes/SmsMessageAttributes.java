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

		public Builder withSenderID(String senderID) {
			this.senderID = senderID;
			return this;
		}

		public Builder withOriginationNumber(String originationNumber) {
			this.originationNumber = originationNumber;
			return this;
		}

		public Builder withMaxPrice(String maxPrice) {
			this.maxPrice = maxPrice;
			return this;
		}

		public Builder withSmsType(SmsType smsType) {
			this.smsType = smsType;
			return this;
		}

		public Builder withEntityId(String entityId) {
			this.entityId = entityId;
			return this;
		}

		public Builder withTemplateId(String templateId) {
			this.templateId = templateId;
			return this;
		}

		public Builder withMessageGroupId(String messageGroupId) {
			this.messageGroupId = messageGroupId;
			return this;
		}

		public Builder withDeduplicationId(String deduplicationId) {
			this.deduplicationId = deduplicationId;
			return this;
		}

		public Builder withMessageStructure(String messageStructure) {
			this.messageStructure = messageStructure;
			return this;
		}

		public Builder withAdm(ADM adm) {
			this.adm = adm;
			return this;
		}

		public Builder withApn(APN apn) {
			this.apn = apn;
			return this;
		}

		public Builder withBaidu(Baidu baidu) {
			this.baidu = baidu;
			return this;
		}

		public Builder withFcm(FCM fcm) {
			this.fcm = fcm;
			return this;
		}

		public Builder withMacOS(MacOS macOS) {
			this.macOS = macOS;
			return this;
		}

		public Builder withMpns(MPNS mpns) {
			this.mpns = mpns;
			return this;
		}

		public Builder withWns(WNS wns) {
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
