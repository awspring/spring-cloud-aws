package io.awspring.cloud.sqs.support.converter.context;

import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesAware;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageConversionContext implements MessageConversionContext, SqsAsyncClientAware, QueueAttributesAware {

	private QueueAttributes queueAttributes;

	private SqsAsyncClient sqsAsyncClient;

	@Override
	public void setQueueAttributes(QueueAttributes queueAttributes) {
		this.queueAttributes = queueAttributes;
	}

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
	}

	public SqsAsyncClient getSqsAsyncClient() {
		return this.sqsAsyncClient;
	}

	public QueueAttributes getQueueAttributes() {
		return this.queueAttributes;
	}
}
