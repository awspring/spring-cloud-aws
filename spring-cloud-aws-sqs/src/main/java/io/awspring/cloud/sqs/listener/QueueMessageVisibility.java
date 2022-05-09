/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.sqs.listener;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link Visibility} implementation for SQS messages.
 *
 * @author Szymon Dembek
 * @author Tomaz Fernandes
 * @since 1.3
 */
public class QueueMessageVisibility implements Visibility {

	private static final Logger logger = LoggerFactory.getLogger(QueueMessageVisibility.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private final String receiptHandle;

	/**
	 * Create an instance for changing the visibility for the provided queue.
	 * @param amazonSqsAsync the client to be used.
	 * @param queueUrl the queue url.
	 * @param receiptHandle the message receipt handle.
	 */
	public QueueMessageVisibility(SqsAsyncClient amazonSqsAsync, String queueUrl, String receiptHandle) {
		this.sqsAsyncClient = amazonSqsAsync;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
	}

	@Override
	public CompletableFuture<Void> changeTo(int seconds) {
		return this.sqsAsyncClient
				.changeMessageVisibility(
						req -> req.queueUrl(this.queueUrl).receiptHandle(this.receiptHandle).visibilityTimeout(seconds))
				.thenRun(() -> logger.trace("Changed the visibility of message {} to {} seconds", this.receiptHandle,
						seconds));
	}
}
