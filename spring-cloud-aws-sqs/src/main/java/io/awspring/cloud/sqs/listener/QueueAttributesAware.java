package io.awspring.cloud.sqs.listener;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface QueueAttributesAware {

	void setQueueAttributes(QueueAttributes queueAttributes);

}
