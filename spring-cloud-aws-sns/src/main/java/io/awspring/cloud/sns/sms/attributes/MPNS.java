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


	public static final class Builder {
		private String notificationClass;
		private Long ttl;
		private String type;

		private Builder() {
		}

		public static Builder aMPNS() {
			return new Builder();
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
