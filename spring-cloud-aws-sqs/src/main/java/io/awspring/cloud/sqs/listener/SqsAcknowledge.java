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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.messaging.support.listener.acknowledgement.AsyncAcknowledgement;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsAcknowledge implements AsyncAcknowledgement {

	private static final Logger logger = LoggerFactory.getLogger(SqsAcknowledge.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private final String receiptHandle;

	public SqsAcknowledge(SqsAsyncClient sqsAsyncClient, String queueUrl, String receiptHandle) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
	}

	@Override
	public CompletableFuture<Void> acknowledge() {
		return this.sqsAsyncClient.deleteMessage(req -> req.queueUrl(this.queueUrl).receiptHandle(this.receiptHandle))
			.thenRun(() -> logger.trace("Acknowledged message with handle {} from queue {}", this.receiptHandle, this.queueUrl));
	}
}
