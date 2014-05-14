package org.elasticspring.messaging.support;

/**
 * @author Agim Emruli
 * @author Alain Sahli
*/
public class NotificationMessage {

	private final String body;
	private final String subject;

	public NotificationMessage(String body, String subject) {
		this.body = body;
		this.subject = subject;
	}

	public String getBody() {
		return this.body;
	}

	public String getSubject() {
		return this.subject;
	}
}
