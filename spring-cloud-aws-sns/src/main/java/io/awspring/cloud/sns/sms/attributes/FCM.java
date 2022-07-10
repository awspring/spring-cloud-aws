package io.awspring.cloud.sns.sms.attributes;

public class FCM {
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


	public static final class Builder {
		private Long fcmTtl;
		private Long gcmTtl;

		private Builder() {
		}

		public static Builder aFCM() {
			return new Builder();
		}

		public Builder withFcmTtl(Long fcmTtl) {
			this.fcmTtl = fcmTtl;
			return this;
		}

		public Builder withGcmTtl(Long gcmTtl) {
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
