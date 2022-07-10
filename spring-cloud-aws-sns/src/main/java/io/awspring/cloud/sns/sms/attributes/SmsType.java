package io.awspring.cloud.sns.sms.attributes;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public enum SmsType {
	PROMOTIONAL("Promotional"), TRANSACTIONAL("Transactional ");
	public final String type;
	SmsType(String type) {
		this.type = type;
	}
}
