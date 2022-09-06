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
package io.awspring.cloud.sns.sms;

/**
 * Helper class that is used to reference field of {@link SmsMessageAttributes} to key that will be used in
 * {@link software.amazon.awssdk.services.sns.model.PublishRequest}.
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
class AttributeCodes {

	private AttributeCodes() {
	}

	static final String SENDER_ID = "AWS.SNS.SMS.SenderID";
	static final String ORIGINATION_NUMBER = "AWS.MM.SMS.OriginationNumber";
	static final String MAX_PRICE = "AWS.SNS.SMS.MaxPrice";
	static final String SMS_TYPE = "AWS.SNS.SMS.SMSType";
	static final String ENTITY_ID = "AWS.MM.SMS.EntityId";
	static final String TEMPLATE_ID = "AWS.MM.SMS.TemplateId";

}
