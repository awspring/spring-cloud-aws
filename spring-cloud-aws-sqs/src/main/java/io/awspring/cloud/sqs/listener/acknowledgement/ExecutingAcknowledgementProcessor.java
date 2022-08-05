package io.awspring.cloud.sqs.listener.acknowledgement;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ExecutingAcknowledgementProcessor<T> extends AcknowledgementProcessor<T> {

	void setAcknowledgementExecutor(AcknowledgementExecutor<T> acknowledgementExecutor);

	void setAcknowledgementOrdering(AcknowledgementOrdering acknowledgementOrdering);

}
