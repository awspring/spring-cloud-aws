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

	public Map<String, MessageAttributeValue> convert() {
		Map<String, MessageAttributeValue> map = new HashMap<>();
		populateMapWithStringValue(AttributeCodes.SENDER_ID, this.getSenderID(), map);
		populateMapWithStringValue(AttributeCodes.ORIGINATION_NUMBER, this.getOriginationNumber(), map);
		populateMapWithNumberValue(AttributeCodes.MAX_PRICE, this.getMaxPrice(), map);
		populateMapWithStringValue(AttributeCodes.SMS_TYPE, this.getSmsType() != null ? this.getSmsType().type : null,
				map);
		populateMapWithStringValue(AttributeCodes.ENTITY_ID, this.getEntityId(), map);
		populateMapWithStringValue(AttributeCodes.TEMPLATE_ID, this.getTemplateId(), map);
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

		public SmsMessageAttributes build() {
			SmsMessageAttributes smsMessageAttributes = new SmsMessageAttributes();
			smsMessageAttributes.setSenderID(senderID);
			smsMessageAttributes.setOriginationNumber(originationNumber);
			smsMessageAttributes.setMaxPrice(maxPrice);
			smsMessageAttributes.setSmsType(smsType);
			smsMessageAttributes.setEntityId(entityId);
			smsMessageAttributes.setTemplateId(templateId);
			return smsMessageAttributes;
		}
	}
}
