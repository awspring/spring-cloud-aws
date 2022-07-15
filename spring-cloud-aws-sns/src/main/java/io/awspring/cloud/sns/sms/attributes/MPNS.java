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

public class MPNS {
	private String notificationClass;
	private Long ttl;
	private String type;

	public String getNotificationClass() {
		return notificationClass;
	}

	public void setNotificationClass(String notificationClass) {
		this.notificationClass = notificationClass;
	}

	public Long getTtl() {
		return ttl;
	}

	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String notificationClass;
		private Long ttl;
		private String type;

		private Builder() {
		}

		public Builder withNotificationClass(String notificationClass) {
			this.notificationClass = notificationClass;
			return this;
		}

		public Builder withTtl(Long ttl) {
			this.ttl = ttl;
			return this;
		}

		public Builder withType(String type) {
			this.type = type;
			return this;
		}

		public MPNS build() {
			MPNS mPNS = new MPNS();
			mPNS.setNotificationClass(notificationClass);
			mPNS.setTtl(ttl);
			mPNS.setType(type);
			return mPNS;
		}
	}
}
