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
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * Helper class that is transformed to {@link MessageAttributeValue} when sending SMS via SNS.
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SmsMessageAttributes {
	/**
	 * The sender ID appears as the message sender on the receiving device. For example your business brand.
	 */
	@Nullable
	private String senderID;
	/**
	 * This OriginationNumber appears as the sender's phone number on the receiving device. The string must match an
	 * origination number that's configured in your AWS account for the destination country.
	 */
	@Nullable
	private String originationNumber;
	/**
	 * The maximum price in USD that you're willing to spend to send the SMS message.
	 */
	@Nullable
	private String maxPrice;
	/**
	 * The type of message that you're sending. This is your entity ID or principal entity (PE) ID for sending SMS
	 * messages to recipients in India.
	 */
	@Nullable
	private SmsType smsType;
	/**
	 * This attribute is required only for sending SMS messages to recipients in India. This is your entity ID or
	 * principal entity (PE) ID for sending SMS messages to recipients in India.
	 */
	@Nullable
	private String entityId;
	/**
	 * This attribute is required only for sending SMS messages to recipients in India. This is your template for
	 * sending SMS messages to recipients in India.
	 */
	@Nullable
	private String templateId;

	public Map<String, MessageAttributeValue> convertAndPopulate() {
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

	@Nullable
	public String getSenderID() {
		return senderID;
	}

	public void setSenderID(@Nullable String senderID) {
		this.senderID = senderID;
	}

	@Nullable
	public String getOriginationNumber() {
		return originationNumber;
	}

	public void setOriginationNumber(@Nullable String originationNumber) {
		this.originationNumber = originationNumber;
	}

	@Nullable
	public String getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(@Nullable String maxPrice) {
		this.maxPrice = maxPrice;
	}

	@Nullable
	public SmsType getSmsType() {
		return smsType;
	}

	public void setSmsType(@Nullable SmsType smsType) {
		this.smsType = smsType;
	}

	@Nullable
	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(@Nullable String entityId) {
		this.entityId = entityId;
	}

	@Nullable
	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(@Nullable String templateId) {
		this.templateId = templateId;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		@Nullable
		private String senderID;
		@Nullable
		private String originationNumber;
		@Nullable
		private String maxPrice;
		@Nullable
		private SmsType smsType;
		@Nullable
		private String entityId;
		@Nullable
		private String templateId;

		private Builder() {
		}

		public Builder senderID(@Nullable String senderID) {
			this.senderID = senderID;
			return this;
		}

		public Builder originationNumber(@Nullable String originationNumber) {
			this.originationNumber = originationNumber;
			return this;
		}

		public Builder maxPrice(@Nullable String maxPrice) {
			this.maxPrice = maxPrice;
			return this;
		}

		public Builder smsType(@Nullable SmsType smsType) {
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
