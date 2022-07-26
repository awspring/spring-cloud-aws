package io.awspring.cloud.sqs.listener;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface SqsAsyncClientAware {

	void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient);

}
