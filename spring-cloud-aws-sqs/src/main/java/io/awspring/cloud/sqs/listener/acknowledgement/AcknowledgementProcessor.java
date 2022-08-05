package io.awspring.cloud.sqs.listener.acknowledgement;

import org.springframework.context.SmartLifecycle;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AcknowledgementProcessor<T> extends SmartLifecycle {

	AcknowledgementCallback<T> getAcknowledgementCallback();

}
