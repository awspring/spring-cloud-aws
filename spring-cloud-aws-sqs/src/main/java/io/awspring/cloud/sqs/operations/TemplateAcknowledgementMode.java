package io.awspring.cloud.sqs.operations;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;

/**
 * Acknowledgement modes to be used by a {@link org.springframework.messaging.core.MessageReceivingOperations}
 * implementation.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public enum TemplateAcknowledgementMode {

	/**
	 * Don't acknowledge messages automatically. The message or messages can be
	 * acknowledged with {@link io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement#acknowledge}
	 * or {@link Acknowledgement#acknowledgeAsync()}
	 */
	DO_NOT_ACKNOWLEDGE,

	/**
	 * Acknowledge received messages automatically.
	 */
	ACKNOWLEDGE_ON_RECEIVE

}
