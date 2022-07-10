package io.awspring.cloud.sns.sms.attributes;

public class MacOS {
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

	public static final class Builder {
		private Long sandboxTtl;
		private Long ttl;

		private Builder() {
		}

		public static Builder aMacOS() {
			return new Builder();
		}

		public Builder withSandboxTtl(Long sandboxTtl) {
			this.sandboxTtl = sandboxTtl;
			return this;
		}

		public Builder withTtl(Long ttl) {
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
