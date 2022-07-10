package io.awspring.cloud.sns.sms.attributes;

public class ADM {
	private Long ttl;

	public Long getTtl() {
		return ttl;
	}

	public void setTtl(Long ttl) {
		this.ttl = ttl;
	}


	public static final class Builder {
		private Long ttl;

		private Builder() {
		}

		public static Builder anADM() {
			return new Builder();
		}

		public Builder withTtl(Long ttl) {
			this.ttl = ttl;
			return this;
		}

		public ADM build() {
			ADM aDM = new ADM();
			aDM.setTtl(ttl);
			return aDM;
		}
	}
}
