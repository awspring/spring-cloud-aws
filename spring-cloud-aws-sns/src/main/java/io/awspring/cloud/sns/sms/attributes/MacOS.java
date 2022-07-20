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

public final class MacOS implements ConvertToMessageAttributes {
	private Long sandboxTtl;
	private Long ttl;

	public Long getSandboxTtl() {
		return sandboxTtl;
	}

	public void setSandboxTtl(Long sandboxTtl) {
		this.sandboxTtl = sandboxTtl;
	}

	public Long getTtl() {
		return ttl;
	}

	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void convertAndPopulate(Map<String, MessageAttributeValue> attributeValueMap) {
		ConvertToMessageAttributes.populateMapWithNumberValue(AttributeCodes.MACOS_TTL, this.getTtl(),
				attributeValueMap);
		ConvertToMessageAttributes.populateMapWithNumberValue(AttributeCodes.MACOS_SANDBOX_TTL, this.getSandboxTtl(),
				attributeValueMap);
	}

	public static final class Builder {
		private Long sandboxTtl;
		private Long ttl;

		private Builder() {
		}

		public Builder sandboxTtl(Long sandboxTtl) {
			this.sandboxTtl = sandboxTtl;
			return this;
		}

		public Builder ttl(Long ttl) {
			this.ttl = ttl;
			return this;
		}

		public MacOS build() {
			MacOS macOS = new MacOS();
			macOS.setSandboxTtl(sandboxTtl);
			macOS.setTtl(ttl);
			return macOS;
		}
	}
}
