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

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.QueueAttributesResolver;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesAware;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementExecutor;
import io.awspring.cloud.sqs.listener.acknowledgement.ExecutingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.SqsAcknowledgementExecutor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.awspring.cloud.sqs.operations.BatchingSqsClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
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
 * @author Heechul Kang
 * @since 3.0
 */
public abstract class AbstractSqsMessageSource<T> extends AbstractPollingMessageSource<T, Message>
		implements SqsAsyncClientAware {

	private static final Logger logger = LoggerFactory.getLogger(AbstractSqsMessageSource.class);

	private static final int MESSAGE_VISIBILITY_DISABLED = -1;

	private SqsAsyncClient sqsAsyncClient;

	private String queueUrl;

	private QueueAttributes queueAttributes;

	private QueueNotFoundStrategy queueNotFoundStrategy;

	private Collection<QueueAttributeName> queueAttributeNames;

	private Collection<String> messageAttributeNames;

	private Collection<String> messageSystemAttributeNames;

	private int messageVisibility;

	private int pollTimeout;

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null.");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	protected void doConfigure(ContainerOptions<?, ?> containerOptions) {
		Assert.isInstanceOf(SqsContainerOptions.class, containerOptions,
				"containerOptions must be an instance of SqsContainerOptions");
		SqsContainerOptions sqsContainerOptions = (SqsContainerOptions) containerOptions;
		this.pollTimeout = (int) sqsContainerOptions.getPollTimeout().getSeconds();
		this.queueAttributeNames = sqsContainerOptions.getQueueAttributeNames();
		this.messageAttributeNames = sqsContainerOptions.getMessageAttributeNames();
		this.messageSystemAttributeNames = sqsContainerOptions.getMessageSystemAttributeNames();
		this.queueNotFoundStrategy = sqsContainerOptions.getQueueNotFoundStrategy();
		this.messageVisibility = sqsContainerOptions.getMessageVisibility() != null
				? (int) sqsContainerOptions.getMessageVisibility().getSeconds()
				: MESSAGE_VISIBILITY_DISABLED;
	}

	@Override
	protected void doStart() {
		Assert.notNull(this.sqsAsyncClient, "sqsAsyncClient not set");
		Assert.notNull(this.queueAttributeNames, "queueAttributeNames not set");
		this.queueAttributes = resolveQueueAttributes();
		this.queueUrl = this.queueAttributes.getQueueUrl();
		configureConversionContextAndAcknowledgement();
	}

	@SuppressWarnings("unchecked")
	private void configureConversionContextAndAcknowledgement() {
		ConfigUtils.INSTANCE
				.acceptIfInstance(getMessageConversionContext(), SqsAsyncClientAware.class,
						saca -> saca.setSqsAsyncClient(this.sqsAsyncClient))
				.acceptIfInstance(getMessageConversionContext(), QueueAttributesAware.class,
						qaa -> qaa.setQueueAttributes(this.queueAttributes))
				.acceptIfInstance(getAcknowledgmentProcessor(), ExecutingAcknowledgementProcessor.class, eap -> eap
						.setAcknowledgementExecutor(createAndConfigureAcknowledgementExecutor(this.queueAttributes)));
	}

	// @formatter:off
	private QueueAttributes resolveQueueAttributes() {
		return QueueAttributesResolver.builder()
			.queueName(getPollingEndpointName())
			.sqsAsyncClient(this.sqsAsyncClient)
			.queueAttributeNames(this.queueAttributeNames)
			.queueNotFoundStrategy(this.queueNotFoundStrategy).build()
			.resolveQueueAttributes()
			.join();
	}
	// @formatter:on

	protected AcknowledgementExecutor<T> createAndConfigureAcknowledgementExecutor(QueueAttributes queueAttributes) {
		AcknowledgementExecutor<T> executor = createAcknowledgementExecutorInstance();
		ConfigUtils.INSTANCE
				.acceptIfInstance(executor, QueueAttributesAware.class, qaa -> qaa.setQueueAttributes(queueAttributes))
				.acceptIfInstance(executor, SqsAsyncClientAware.class,
						saca -> saca.setSqsAsyncClient(this.sqsAsyncClient));
		return executor;
	}

	protected AcknowledgementExecutor<T> createAcknowledgementExecutorInstance() {
		return new SqsAcknowledgementExecutor<>();
	}

	// @formatter:off
	@Override
	protected CompletableFuture<Collection<Message>> doPollForMessages(
			int maxNumberOfMessages) {
		logger.debug("Polling queue {} for {} messages.", this.queueUrl, maxNumberOfMessages);
		return maxNumberOfMessages <= 10
			? executePoll(maxNumberOfMessages)
			: executeMultiplePolls(maxNumberOfMessages);
	}

	private CompletableFuture<Collection<Message>> executePoll(int maxNumberOfMessages) {
		return sqsAsyncClient
			.receiveMessage(createRequest(maxNumberOfMessages))
			.thenApply(ReceiveMessageResponse::messages)
			.thenApply(collectionList -> (Collection<Message>) collectionList)
			.whenComplete(this::logMessagesReceived);
	}

	private ReceiveMessageRequest createRequest(int maxNumberOfMessages) {
		ReceiveMessageRequest.Builder builder = ReceiveMessageRequest
			.builder()
			.queueUrl(this.queueUrl)
			.maxNumberOfMessages(maxNumberOfMessages)
			.waitTimeSeconds(this.pollTimeout);
		customizeRequest(builder);
		if (this.messageVisibility >= 0) {
			builder.visibilityTimeout(this.messageVisibility);
		}
        if (!(this.sqsAsyncClient instanceof BatchingSqsClientAdapter)) {
            builder.messageAttributeNames(this.messageAttributeNames)
                    .attributeNamesWithStrings(this.messageSystemAttributeNames);
        }
		return builder.build();
	}
	// @formatter:on

	protected void customizeRequest(ReceiveMessageRequest.Builder builder) {
	}

	private CompletableFuture<Collection<Message>> executeMultiplePolls(int maxNumberOfMessages) {
		int remainder = maxNumberOfMessages % 10;
		return remainder == 0 ? combinePolls(maxNumberOfMessages)
				: combinePolls(maxNumberOfMessages).thenCombine(executePoll(remainder), this::combineBatches);
	}

	private CompletableFuture<Collection<Message>> combinePolls(int maxNumberOfMessages) {
		return IntStream.range(0, maxNumberOfMessages / 10).mapToObj(index -> executePoll(10)).reduce(
				CompletableFuture.completedFuture(Collections.emptyList()),
				(first, second) -> first.thenCombine(second, this::combineBatches));
	}

	private Collection<Message> combineBatches(Collection<Message> firstBatch, Collection<Message> secondBatch) {
		List<Message> combinedBatch = new ArrayList<>(firstBatch);
		combinedBatch.addAll(secondBatch);
		return combinedBatch;
	}

	private void logMessagesReceived(@Nullable Collection<Message> v, @Nullable Throwable t) {
		if (v != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Received {} messages {} from queue {}", v.size(),
						v.stream().map(Message::messageId).collect(Collectors.toList()), this.queueUrl);
			}
			else {
				logger.debug("Received {} messages from queue {}", v.size(), this.queueUrl);
			}
		}
	}

}
