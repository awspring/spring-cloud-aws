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

	public static final class Builder {
		private String deployStatus;
		private String messageKey;
		private String messageType;
		private Long ttl;

		private Builder() {
		}

		public static Builder aBaidu() {
			return new Builder();
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
