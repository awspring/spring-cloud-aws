/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.CompletableFutures;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@link Acknowledgement} implementation for Sqs messages. Acknowledges by deleting the message from the queue.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsAcknowledgement implements AsyncAcknowledgement, Acknowledgement {

	private static final Logger logger = LoggerFactory.getLogger(SqsAcknowledgement.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private final String receiptHandle;
	private final String messageId;

	/**
	 * Create an instance with the supplied parameters.
	 * @param sqsAsyncClient the {@link SqsAsyncClient} to be used to acknowledge the message.
	 * @param queueUrl the url for the queue containing the message.
	 * @param receiptHandle the handle for the SQS message to be deleted.
	 * @param s
	 */
	public SqsAcknowledgement(SqsAsyncClient sqsAsyncClient, String queueUrl, String receiptHandle, String messageId) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		Assert.notNull(queueUrl, "queueUrl cannot be null");
		Assert.notNull(receiptHandle, "receiptHandle cannot be null");
		Assert.notNull(messageId, "messageId cannot be null");
		this.sqsAsyncClient = sqsAsyncClient;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
		this.messageId = messageId;
	}

	@Override
	public CompletableFuture<Void> acknowledgeAsync() {
		return CompletableFutures.exceptionallyCompose(doAcknowledgeAsync(),
				t -> CompletableFutures.failedFuture(createAcknowledgementException("Error acknowledging message", t)));
	}

	// @formatter:off
	private CompletableFuture<Void> doAcknowledgeAsync() {
		return this.sqsAsyncClient
			.deleteMessage(req -> req
				.queueUrl(this.queueUrl)
				.receiptHandle(this.receiptHandle))
			.thenRun(() -> logger.trace("Acknowledged message {} from queue {}", this.messageId, this.queueUrl));
	}
	// @formatter:on

	@Override
	public void acknowledge() {
		try {
			doAcknowledgeAsync().get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw createAcknowledgementException("Interrupted while acknowledging message", e);
		}
		catch (Exception e) {
			throw createAcknowledgementException("Error acknowledging message", e);
		}
	}

	private SqsAcknowledgementException createAcknowledgementException(String message, Throwable t) {
		return new SqsAcknowledgementException(message + " MessageId: " + this.messageId, Collections.emptyList(),
				this.queueUrl, t);
	}
}
