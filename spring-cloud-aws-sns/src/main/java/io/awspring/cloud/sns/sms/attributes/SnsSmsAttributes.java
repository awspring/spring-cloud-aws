package io.awspring.cloud.sns.sms.attributes;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SnsSmsAttributes {
	private String senderID;
	private String originationNumber;
	private String maxPrice;
	private SmsType smsType;
	private String entityId;
	private String templateId;
	private String messageGroupId;
	private String deduplicationId;
	private String messageStructure;
	private ADM adm;
	private APN apn;
	private Baidu baidu;
	private FCM fcm;
	private MacOS macOS;
	private MPNS mpns;
	private WNS wns;

	public String getSenderID() {
		return senderID;
	}

	public void setSenderID(String senderID) {
		this.senderID = senderID;
	}

	public String getOriginationNumber() {
		return originationNumber;
	}

	public void setOriginationNumber(String originationNumber) {
		this.originationNumber = originationNumber;
	}

	public String getMaxPrice() {
		return maxPrice;
	}

	public void setMaxPrice(String maxPrice) {
		this.maxPrice = maxPrice;
	}

	public SmsType getSmsType() {
		return smsType;
	}

	public void setSmsType(SmsType smsType) {
		this.smsType = smsType;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public void setMessageGroupId(String messageGroupId) {
		this.messageGroupId = messageGroupId;
	}

	public void setDeduplicationId(String deduplicationId) {
		this.deduplicationId = deduplicationId;
	}

	public void setMessageStructure(String messageStructure) {
		this.messageStructure = messageStructure;
	}

	public void setAdm(ADM adm) {
		this.adm = adm;
	}

	public void setApn(APN apn) {
		this.apn = apn;
	}

	public void setBaidu(Baidu baidu) {
		this.baidu = baidu;
	}

	public void setFcm(FCM fcm) {
		this.fcm = fcm;
	}

	public void setMacOS(MacOS macOS) {
		this.macOS = macOS;
	}

	public void setMpns(MPNS mpns) {
		this.mpns = mpns;
	}

	public void setWns(WNS wns) {
		this.wns = wns;
	}


	public static final class Builder {
		private String senderID;
		private String originationNumber;
		private String maxPrice;
		private SmsType smsType;
		private String entityId;
		private String templateId;
		private String messageGroupId;
		private String deduplicationId;
		private String messageStructure;
		private ADM adm;
		private APN apn;
		private Baidu baidu;
		private FCM fcm;
		private MacOS macOS;
		private MPNS mpns;
		private WNS wns;

		private Builder() {
		}

		public static Builder aSnsSmsAttributes() {
			return new Builder();
		}

		public Builder withSenderID(String senderID) {
			this.senderID = senderID;
			return this;
		}

		public Builder withOriginationNumber(String originationNumber) {
			this.originationNumber = originationNumber;
			return this;
		}

		public Builder withMaxPrice(String maxPrice) {
			this.maxPrice = maxPrice;
			return this;
		}

		public Builder withSmsType(SmsType smsType) {
			this.smsType = smsType;
			return this;
		}

		public Builder withEntityId(String entityId) {
			this.entityId = entityId;
			return this;
		}

		public Builder withTemplateId(String templateId) {
			this.templateId = templateId;
			return this;
		}

		public Builder withMessageGroupId(String messageGroupId) {
			this.messageGroupId = messageGroupId;
			return this;
		}

		public Builder withDeduplicationId(String deduplicationId) {
			this.deduplicationId = deduplicationId;
			return this;
		}

		public Builder withMessageStructure(String messageStructure) {
			this.messageStructure = messageStructure;
			return this;
		}

		public Builder withAdm(ADM adm) {
			this.adm = adm;
			return this;
		}

		public Builder withApn(APN apn) {
			this.apn = apn;
			return this;
		}

		public Builder withBaidu(Baidu baidu) {
			this.baidu = baidu;
			return this;
		}

		public Builder withFcm(FCM fcm) {
			this.fcm = fcm;
			return this;
		}

		public Builder withMacOS(MacOS macOS) {
			this.macOS = macOS;
			return this;
		}

		public Builder withMpns(MPNS mpns) {
			this.mpns = mpns;
			return this;
		}

		public Builder withWns(WNS wns) {
			this.wns = wns;
			return this;
		}

		public SnsSmsAttributes build() {
			SnsSmsAttributes snsSmsAttributes = new SnsSmsAttributes();
			snsSmsAttributes.setSenderID(senderID);
			snsSmsAttributes.setOriginationNumber(originationNumber);
			snsSmsAttributes.setMaxPrice(maxPrice);
			snsSmsAttributes.setSmsType(smsType);
			snsSmsAttributes.setEntityId(entityId);
			snsSmsAttributes.setTemplateId(templateId);
			snsSmsAttributes.setMessageGroupId(messageGroupId);
			snsSmsAttributes.setDeduplicationId(deduplicationId);
			snsSmsAttributes.setMessageStructure(messageStructure);
			snsSmsAttributes.setAdm(adm);
			snsSmsAttributes.setApn(apn);
			snsSmsAttributes.setBaidu(baidu);
			snsSmsAttributes.setFcm(fcm);
			snsSmsAttributes.setMacOS(macOS);
			snsSmsAttributes.setMpns(mpns);
			snsSmsAttributes.setWns(wns);
			return snsSmsAttributes;
		}
	}
}
