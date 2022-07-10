package io.awspring.cloud.sns.sms.attributes;

public class APN {
	private Long mdmTtl;
	private Long mdmSandboxTtl;
	private Long passbookTtl;
	private Long passbookSandboxTtl;
	private Long voipTtl;
	private Long voipSandboxTtl;
	private Long collapseId;
	private String priority;
	private String pushType;
	private String topic;
	private Long sandboxTtl;
	private String ttl;

	public Long getMdmTtl() {
		return mdmTtl;
	}

	public void setMdmTtl(Long mdmTtl) {
		this.mdmTtl = mdmTtl;
	}

	public Long getMdmSandboxTtl() {
		return mdmSandboxTtl;
	}

	public void setMdmSandboxTtl(Long mdmSandboxTtl) {
		this.mdmSandboxTtl = mdmSandboxTtl;
	}

	public Long getPassbookTtl() {
		return passbookTtl;
	}

	public void setPassbookTtl(Long passbookTtl) {
		this.passbookTtl = passbookTtl;
	}

	public Long getPassbookSandboxTtl() {
		return passbookSandboxTtl;
	}

	public void setPassbookSandboxTtl(Long passbookSandboxTtl) {
		this.passbookSandboxTtl = passbookSandboxTtl;
	}

	public Long getVoipTtl() {
		return voipTtl;
	}

	public void setVoipTtl(Long voipTtl) {
		this.voipTtl = voipTtl;
	}

	public Long getVoipSandboxTtl() {
		return voipSandboxTtl;
	}

	public void setVoipSandboxTtl(Long voipSandboxTtl) {
		this.voipSandboxTtl = voipSandboxTtl;
	}

	public Long getCollapseId() {
		return collapseId;
	}

	public void setCollapseId(Long collapseId) {
		this.collapseId = collapseId;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getPushType() {
		return pushType;
	}

	public void setPushType(String pushType) {
		this.pushType = pushType;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Long getSandboxTtl() {
		return sandboxTtl;
	}

	public void setSandboxTtl(Long sandboxTtl) {
		this.sandboxTtl = sandboxTtl;
	}

	public String getTtl() {
		return ttl;
	}

	public void setTtl(String ttl) {
		this.ttl = ttl;
	}

	public static final class Builder {
		private Long mdmTtl;
		private Long mdmSandboxTtl;
		private Long passbookTtl;
		private Long passbookSandboxTtl;
		private Long voipTtl;
		private Long voipSandboxTtl;
		private Long collapseId;
		private String priority;
		private String pushType;
		private String topic;
		private Long sandboxTtl;
		private String ttl;

		private Builder() {
		}

		public static Builder anAPN() {
			return new Builder();
		}

		public Builder withMdmTtl(Long mdmTtl) {
			this.mdmTtl = mdmTtl;
			return this;
		}

		public Builder withMdmSandboxTtl(Long mdmSandboxTtl) {
			this.mdmSandboxTtl = mdmSandboxTtl;
			return this;
		}

		public Builder withPassbookTtl(Long passbookTtl) {
			this.passbookTtl = passbookTtl;
			return this;
		}

		public Builder withPassbookSandboxTtl(Long passbookSandboxTtl) {
			this.passbookSandboxTtl = passbookSandboxTtl;
			return this;
		}

		public Builder withVoipTtl(Long voipTtl) {
			this.voipTtl = voipTtl;
			return this;
		}

		public Builder withVoipSandboxTtl(Long voipSandboxTtl) {
			this.voipSandboxTtl = voipSandboxTtl;
			return this;
		}

		public Builder withCollapseId(Long collapseId) {
			this.collapseId = collapseId;
			return this;
		}

		public Builder withPriority(String priority) {
			this.priority = priority;
			return this;
		}

		public Builder withPushType(String pushType) {
			this.pushType = pushType;
			return this;
		}

		public Builder withTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder withSandboxTtl(Long sandboxTtl) {
			this.sandboxTtl = sandboxTtl;
			return this;
		}

		public Builder withTtl(String ttl) {
			this.ttl = ttl;
			return this;
		}

		public APN build() {
			APN aPN = new APN();
			aPN.setMdmTtl(mdmTtl);
			aPN.setMdmSandboxTtl(mdmSandboxTtl);
			aPN.setPassbookTtl(passbookTtl);
			aPN.setPassbookSandboxTtl(passbookSandboxTtl);
			aPN.setVoipTtl(voipTtl);
			aPN.setVoipSandboxTtl(voipSandboxTtl);
			aPN.setCollapseId(collapseId);
			aPN.setPriority(priority);
			aPN.setPushType(pushType);
			aPN.setTopic(topic);
			aPN.setSandboxTtl(sandboxTtl);
			aPN.setTtl(ttl);
			return aPN;
		}
	}
}
