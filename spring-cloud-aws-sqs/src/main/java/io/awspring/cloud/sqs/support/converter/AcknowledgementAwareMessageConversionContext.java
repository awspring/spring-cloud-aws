package io.awspring.cloud.sqs.support.converter;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AcknowledgementAwareMessageConversionContext extends MessageConversionContext {

	void setAcknowledgementCallback(AcknowledgementCallback<?> acknowledgementCallback);

}
