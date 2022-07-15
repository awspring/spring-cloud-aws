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

public class Baidu {
	private String deployStatus;
	private String messageKey;
	private String messageType;
	private Long ttl;

	public String getDeployStatus() {
		return deployStatus;
	}

	public void setDeployStatus(String deployStatus) {
		this.deployStatus = deployStatus;
	}

	public String getMessageKey() {
		return messageKey;
	}

	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
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

	public static final class Builder {
		private String deployStatus;
		private String messageKey;
		private String messageType;
		private Long ttl;

		private Builder() {
		}

		public Builder withDeployStatus(String deployStatus) {
			this.deployStatus = deployStatus;
			return this;
		}

		public Builder withMessageKey(String messageKey) {
			this.messageKey = messageKey;
			return this;
		}

		public Builder withMessageType(String messageType) {
			this.messageType = messageType;
			return this;
		}

		public Builder withTtl(Long ttl) {
			this.ttl = ttl;
			return this;
		}

		public Baidu build() {
			Baidu baidu = new Baidu();
			baidu.setDeployStatus(deployStatus);
			baidu.setMessageKey(messageKey);
			baidu.setMessageType(messageType);
			baidu.setTtl(ttl);
			return baidu;
		}
	}
}
