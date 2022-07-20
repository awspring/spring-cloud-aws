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

import java.util.Map;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

public final class FCM implements ConvertToMessageAttributes {
	private Long fcmTtl;
	private Long gcmTtl;

	public Long getFcmTtl() {
		return fcmTtl;
	}

	public void setFcmTtl(Long fcmTtl) {
		this.fcmTtl = fcmTtl;
	}

	public Long getGcmTtl() {
		return gcmTtl;
	}

	public void setGcmTtl(Long gcmTtl) {
		this.gcmTtl = gcmTtl;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void convertAndPopulate(Map<String, MessageAttributeValue> attributeValueMap) {
		ConvertToMessageAttributes.populateMapWithNumberValue(AttributeCodes.FCM_TTL, this.getFcmTtl(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithNumberValue(AttributeCodes.GCM_TTL, this.getGcmTtl(),
				attributeValueMap);
	}

	public static final class Builder {
		private Long fcmTtl;
		private Long gcmTtl;

		private Builder() {
		}

		public Builder fcmTtl(Long fcmTtl) {
			this.fcmTtl = fcmTtl;
			return this;
		}

		public Builder gcmTtl(Long gcmTtl) {
			this.gcmTtl = gcmTtl;
			return this;
		}

		public FCM build() {
			FCM fCM = new FCM();
			fCM.setFcmTtl(fcmTtl);
			fCM.setGcmTtl(gcmTtl);
			return fCM;
		}
	}
}
