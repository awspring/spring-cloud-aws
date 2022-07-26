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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.QueueAttributes;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.awspring.cloud.sqs.support.SqsMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * {@link MessageSource} implementation for polling messages from a SQS queue and converting them to messaging
 * {@link Message}.
 *
 * <p>
 * A {@link io.awspring.cloud.sqs.listener.MessageListenerContainer} can contain many sources, and each source polls
 * from a single queue.
 * </p>
 *
 * <p>
 * Note that currently the payload is not converted here and is returned as String. The actual conversion to the
 * {@link io.awspring.cloud.sqs.annotation.SqsListener} argument type happens on
 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod} invocation.
 * </p>
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageSource<T> extends AbstractPollingMessageSource<T> implements SqsAsyncClientAware {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageSource.class);

	private SqsAsyncClient sqsAsyncClient;

	private String queueUrl;

	private SqsMessageConverter<T> sqsMessageConverter;

	private Collection<QueueAttributeName> queueAttributeNames;

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null.");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	public void configure(ContainerOptions containerOptions) {
		super.configure(containerOptions);
		this.queueAttributeNames = containerOptions.getQueueAttributeNames();
	}

	@Override
	protected void doStart() {
		Assert.notNull(this.sqsAsyncClient, "sqsAsyncClient not set.");
		QueueAttributes queueAttributes = QueueAttributes.fetchFor(getPollingEndpointName(), this.sqsAsyncClient);
		this.queueUrl = queueAttributes.getQueueUrl();
		this.sqsMessageConverter = new SqsMessageConverter<>(queueAttributes, this.sqsAsyncClient);
	}

	@Override
	protected CompletableFuture<Collection<Message<T>>> doPollForMessages(int messagesToRequest) {
		logger.debug("Polling queue {} for {} messages.", this.queueUrl, messagesToRequest);
		return sqsAsyncClient
				.receiveMessage(req -> req.queueUrl(this.queueUrl)
					.receiveRequestAttemptId(UUID.randomUUID().toString())
					.maxNumberOfMessages(messagesToRequest)
					.attributeNames(this.queueAttributeNames)
					.waitTimeSeconds(getPollTimeoutSeconds()))
				.thenApply(ReceiveMessageResponse::messages)
				.thenApply(this.sqsMessageConverter::toMessagingMessages)
			.whenComplete((v, t) -> {
				if (v != null) {
					logger.trace("Received {} messages: {} from queue {}", v.size(), MessageHeaderUtils.getId(v), this.queueUrl);
				}
			});
	}
}
