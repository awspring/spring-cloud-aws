/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import io.awspring.cloud.sqs.FifoUtils;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.QueueAttributesResolver;
import io.awspring.cloud.sqs.SqsAcknowledgementException;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.SqsHeaders.MessageSystemAttributes;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.support.converter.MessageAttributeDataTypes;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessageConversionContext;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import io.awspring.cloud.sqs.support.observation.SqsTemplateObservation;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Sqs-specific implementation of {@link AbstractMessagingTemplate}
 *
 * @author Tomaz Fernandes
 * @author Zhong Xi Lu
 *
 * @since 3.0
 */
public class SqsTemplate extends AbstractMessagingTemplate<Message> implements SqsOperations, SqsAsyncOperations {

	private static final Logger logger = LoggerFactory.getLogger(SqsTemplate.class);

	private static final SqsTemplateObservation.SqsSpecifics SQS_OBSERVATION_SPECIFICS = new SqsTemplateObservation.SqsSpecifics();

	private final Map<String, CompletableFuture<QueueAttributes>> queueAttributesCache = new ConcurrentHashMap<>();

	private final Map<String, SqsMessageConversionContext> conversionContextCache = new ConcurrentHashMap<>();

	private final SqsAsyncClient sqsAsyncClient;

	private final Collection<QueueAttributeName> queueAttributeNames;

	private final QueueNotFoundStrategy queueNotFoundStrategy;

	private final Collection<String> messageAttributeNames;

	private final Collection<String> messageSystemAttributeNames;

	private final TemplateContentBasedDeduplication contentBasedDeduplication;

	private SqsTemplate(SqsTemplateBuilderImpl builder) {
		super(builder.messageConverter, builder.options, SQS_OBSERVATION_SPECIFICS);
		SqsTemplateOptionsImpl options = builder.options;
		this.sqsAsyncClient = builder.sqsAsyncClient;
		this.messageAttributeNames = options.messageAttributeNames;
		this.queueAttributeNames = options.queueAttributeNames;
		this.queueNotFoundStrategy = options.queueNotFoundStrategy;
		this.messageSystemAttributeNames = options.messageSystemAttributeNames;
		this.contentBasedDeduplication = options.contentBasedDeduplication;
	}

	/**
	 * Create a new {@link SqsTemplateBuilder}.
	 * @return the builder.
	 */
	public static SqsTemplateBuilder builder() {
		return new SqsTemplateBuilderImpl();
	}

	/**
	 * Create a new {@link SqsTemplate} instance with the provided {@link SqsAsyncClient} and both sync and async
	 * operations.
	 * @param sqsAsyncClient the client to be used by the template.
	 * @return the {@link SqsTemplate} instance.
	 */
	public static SqsTemplate newTemplate(SqsAsyncClient sqsAsyncClient) {
		return new SqsTemplateBuilderImpl().sqsAsyncClient(sqsAsyncClient).build();
	}

	/**
	 * Create a new {@link SqsTemplate} instance with the provided {@link SqsAsyncClient}, only exposing the sync
	 * methods contained in {@link SqsOperations}.
	 * @param sqsAsyncClient the client.
	 * @return the new template instance.
	 */
	public static SqsOperations newSyncTemplate(SqsAsyncClient sqsAsyncClient) {
		return newTemplate(sqsAsyncClient);
	}

	/**
	 * Create a new {@link SqsTemplate} instance with the provided {@link SqsAsyncClient}, only exposing the async
	 * methods contained in {@link SqsAsyncOperations}.
	 *
	 * @param sqsAsyncClient the client.
	 * @return the new template instance.
	 */
	public static SqsAsyncOperations newAsyncTemplate(SqsAsyncClient sqsAsyncClient) {
		return newTemplate(sqsAsyncClient);
	}

	@Override
	public <T> SendResult<T> send(Consumer<SqsSendOptions<T>> to) {
		return unwrapCompletionException(sendAsync(to));
	}

	@Override
	public <T> CompletableFuture<SendResult<T>> sendAsync(Consumer<SqsSendOptions<T>> to) {
		Assert.notNull(to, "to must not be null");
		SqsSendOptionsImpl<T> options = new SqsSendOptionsImpl<>();
		to.accept(options);
		org.springframework.messaging.Message<T> message = messageFromSendOptions(options);
		return sendAsync(options.queue, message);
	}

	private <T> org.springframework.messaging.Message<T> messageFromSendOptions(SqsSendOptionsImpl<T> options) {
		Assert.notNull(options.payload, "payload must not be null");
		MessageBuilder<T> builder = MessageBuilder.withPayload(options.payload).copyHeaders(options.headers);
		if (options.delay != null) {
			builder.setHeader(SqsHeaders.SQS_DELAY_HEADER, options.delay);
		}
		if (options.messageDeduplicationId != null) {
			builder.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER,
					options.messageDeduplicationId);
		}
		if (options.messageGroupId != null) {
			builder.setHeader(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, options.messageGroupId);
		}
		return builder.build();
	}

	@Override
	public Optional<org.springframework.messaging.Message<?>> receive(Consumer<SqsReceiveOptions> from) {
		return unwrapCompletionException(receiveAsync(from));
	}

	@Override
	public <T> Optional<org.springframework.messaging.Message<T>> receive(Consumer<SqsReceiveOptions> from,
			Class<T> payloadClass) {
		return unwrapCompletionException(receiveAsync(from, payloadClass));
	}

	@Override
	public Collection<org.springframework.messaging.Message<?>> receiveMany(Consumer<SqsReceiveOptions> from) {
		return unwrapCompletionException(receiveManyAsync(from));
	}

	@Override
	public <T> Collection<org.springframework.messaging.Message<T>> receiveMany(Consumer<SqsReceiveOptions> from,
			Class<T> payloadClass) {
		return unwrapCompletionException(receiveManyAsync(from, payloadClass));
	}

	@Override
	public CompletableFuture<Optional<org.springframework.messaging.Message<?>>> receiveAsync(
			Consumer<SqsReceiveOptions> from) {
		Assert.notNull(from, "from must not be null");
		SqsReceiveOptionsImpl options = new SqsReceiveOptionsImpl();
		from.accept(options);
		Assert.isTrue(options.maxNumberOfMessages == null || options.maxNumberOfMessages == 1,
				"maxNumberOfMessages must be null or 1. Use receiveMany to receive more messages.");
		Map<String, Object> additionalHeaders = addAdditionalReceiveHeaders(options);
		return receiveAsync(options.queue, null, options.pollTimeout, additionalHeaders);
	}

	@Override
	public <T> CompletableFuture<Optional<org.springframework.messaging.Message<T>>> receiveAsync(
			Consumer<SqsReceiveOptions> from, Class<T> payloadClass) {
		Assert.notNull(from, "from must not be null");
		Assert.notNull(payloadClass, "payloadClass must not be null");
		SqsReceiveOptionsImpl options = new SqsReceiveOptionsImpl();
		from.accept(options);
		Assert.isTrue(options.maxNumberOfMessages == null || options.maxNumberOfMessages == 1,
				"maxNumberOfMessages must be null or 1. Use receiveMany to receive more messages.");
		Map<String, Object> additionalHeaders = addAdditionalReceiveHeaders(options);
		return receiveAsync(options.queue, payloadClass, options.pollTimeout, additionalHeaders)
				.thenApply(super::castFromOptional);
	}

	@Override
	public CompletableFuture<Collection<org.springframework.messaging.Message<?>>> receiveManyAsync(
			Consumer<SqsReceiveOptions> from) {
		Assert.notNull(from, "from must not be null");
		SqsReceiveOptionsImpl options = new SqsReceiveOptionsImpl();
		from.accept(options);
		return receiveManyAsync(options.queue, null, options.pollTimeout, options.maxNumberOfMessages,
				addAdditionalReceiveHeaders(options));
	}

	@Override
	public <T> CompletableFuture<Collection<org.springframework.messaging.Message<T>>> receiveManyAsync(
			Consumer<SqsReceiveOptions> from, Class<T> payloadClass) {
		Assert.notNull(from, "from must not be null");
		Assert.notNull(payloadClass, "payloadClass must not be null");
		SqsReceiveOptionsImpl options = new SqsReceiveOptionsImpl();
		from.accept(options);
		return receiveManyAsync(options.queue, payloadClass, options.pollTimeout, options.maxNumberOfMessages,
				addAdditionalReceiveHeaders(options)).thenApply(super::castFromCollection);
	}

	private Map<String, Object> addAdditionalReceiveHeaders(SqsReceiveOptionsImpl options) {
		Map<String, Object> additionalHeaders = new HashMap<>(options.additionalHeaders);
		if (options.visibilityTimeout != null) {
			additionalHeaders.put(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, options.visibilityTimeout);
		}
		if (options.receiveRequestAttemptId != null) {
			additionalHeaders.put(SqsHeaders.SQS_RECEIVE_REQUEST_ATTEMPT_ID_HEADER, options.receiveRequestAttemptId);
		}
		return additionalHeaders;
	}

	@Override
	protected <T> org.springframework.messaging.Message<T> preProcessMessageForSend(String endpointToUse,
			org.springframework.messaging.Message<T> message) {
		return message;
	}

	@Override
	protected <T> Collection<org.springframework.messaging.Message<T>> preProcessMessagesForSend(String endpointToUse,
			Collection<org.springframework.messaging.Message<T>> messages) {
		return messages;
	}

	@Override
	protected <T> CompletableFuture<org.springframework.messaging.Message<T>> preProcessMessageForSendAsync(
			String endpointToUse, org.springframework.messaging.Message<T> message) {
		return FifoUtils.isFifo(endpointToUse)
				? endpointHasContentBasedDeduplicationEnabled(endpointToUse)
						.thenApply(enabled -> enabled ? addMissingFifoSendHeaders(message, Map.of())
								: addMissingFifoSendHeaders(message, getRandomDeduplicationIdHeader()))
				: CompletableFuture.completedFuture(message);
	}

	@Override
	protected <T> CompletableFuture<Collection<org.springframework.messaging.Message<T>>> preProcessMessagesForSendAsync(
			String endpointToUse, Collection<org.springframework.messaging.Message<T>> messages) {
		return FifoUtils.isFifo(endpointToUse)
				? endpointHasContentBasedDeduplicationEnabled(endpointToUse).thenApply(enabled -> messages.stream()
						.map(message -> enabled ? addMissingFifoSendHeaders(message, Map.of())
								: addMissingFifoSendHeaders(message, getRandomDeduplicationIdHeader()))
						.toList())
				: CompletableFuture.completedFuture(messages);
	}

	private <T> org.springframework.messaging.Message<T> addMissingFifoSendHeaders(
			org.springframework.messaging.Message<T> message, Map<String, Object> additionalHeaders) {
		return MessageHeaderUtils.addHeadersIfAbsent(message,
				Stream.concat(additionalHeaders.entrySet().stream(),
						getRandomMessageGroupIdHeader().entrySet().stream())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	private Map<String, String> getRandomMessageGroupIdHeader() {
		return Map.of(MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, UUID.randomUUID().toString());
	}

	private Map<String, Object> getRandomDeduplicationIdHeader() {
		return Map.of(MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
	}

	private CompletableFuture<Boolean> endpointHasContentBasedDeduplicationEnabled(String endpointName) {
		return TemplateContentBasedDeduplication.AUTO.equals(this.contentBasedDeduplication)
				? handleAutoDeduplication(endpointName)
				: CompletableFuture
						.completedFuture(contentBasedDeduplication.equals(TemplateContentBasedDeduplication.ENABLED));
	}

	private CompletableFuture<Boolean> handleAutoDeduplication(String endpointName) {
		return getQueueAttributes(endpointName).thenApply(attributes -> Boolean
				.parseBoolean(attributes.getQueueAttribute(QueueAttributeName.CONTENT_BASED_DEDUPLICATION)));
	}

	@Override
	protected <T> CompletableFuture<SendResult<T>> doSendAsync(String endpointName, Message message,
			org.springframework.messaging.Message<T> originalMessage) {
		return createSendMessageRequest(endpointName, message).thenCompose(this.sqsAsyncClient::sendMessage)
				.thenApply(response -> createSendResult(UUID.fromString(response.messageId()),
						response.sequenceNumber(), endpointName, originalMessage));
	}

	private <T> SendResult<T> createSendResult(UUID messageId, @Nullable String sequenceNumber, String endpointName,
			org.springframework.messaging.Message<T> originalMessage) {
		return new SendResult<>(messageId, endpointName, originalMessage,
				sequenceNumber != null
						? Collections.singletonMap(SqsTemplateParameters.SEQUENCE_NUMBER_PARAMETER_NAME, sequenceNumber)
						: Collections.emptyMap());
	}

	private CompletableFuture<SendMessageRequest> createSendMessageRequest(String endpointName, Message message) {
		return getQueueAttributes(endpointName)
				.thenApply(queueAttributes -> doCreateSendMessageRequest(message, queueAttributes));
	}

	private SendMessageRequest doCreateSendMessageRequest(Message message, QueueAttributes queueAttributes) {
		return SendMessageRequest.builder().queueUrl(queueAttributes.getQueueUrl()).messageBody(message.body())
				.messageDeduplicationId(message.attributes().get(MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID))
				.messageGroupId(message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID))
				.delaySeconds(getDelaySeconds(message))
				.messageAttributes(excludeKnownFields(message.messageAttributes()))
				.messageSystemAttributes(mapMessageSystemAttributes(message)).build();
	}

	@Override
	protected <T> CompletableFuture<SendResult.Batch<T>> doSendBatchAsync(String endpointName,
			Collection<Message> messages, Collection<org.springframework.messaging.Message<T>> originalMessages) {
		logger.debug("Sending messages {} to endpoint {}", messages, endpointName);
		return createSendMessageBatchRequest(endpointName, messages).thenCompose(this.sqsAsyncClient::sendMessageBatch)
				.thenApply(response -> createSendResultBatch(response, endpointName,
						originalMessages.stream().collect(Collectors.toMap(MessageHeaderUtils::getId, msg -> msg))));
	}

	private <T> SendResult.Batch<T> createSendResultBatch(SendMessageBatchResponse response, String endpointName,
			Map<String, org.springframework.messaging.Message<T>> originalMessagesById) {
		return new SendResult.Batch<>(doCreateSendResultBatch(response, endpointName, originalMessagesById),
				createSendResultFailed(response, endpointName, originalMessagesById));
	}

	private <T> Collection<SendResult.Failed<T>> createSendResultFailed(SendMessageBatchResponse response,
			String endpointName, Map<String, org.springframework.messaging.Message<T>> originalMessagesById) {
		return response.failed().stream()
				.map(entry -> new SendResult.Failed<>(entry.message(), endpointName,
						originalMessagesById.get(entry.id()), Map.of(SqsTemplateParameters.SENDER_FAULT_PARAMETER_NAME,
								entry.senderFault(), SqsTemplateParameters.ERROR_CODE_PARAMETER_NAME, entry.code())))
				.toList();
	}

	private <T> Collection<SendResult<T>> doCreateSendResultBatch(SendMessageBatchResponse response,
			String endpointName, Map<String, org.springframework.messaging.Message<T>> originalMessagesById) {
		return response
				.successful().stream().map(entry -> createSendResult(UUID.fromString(entry.messageId()),
						entry.sequenceNumber(), endpointName, getOriginalMessage(originalMessagesById, entry)))
				.toList();
	}

	private <T> org.springframework.messaging.Message<T> getOriginalMessage(
			Map<String, org.springframework.messaging.Message<T>> originalMessagesById,
			SendMessageBatchResultEntry entry) {
		org.springframework.messaging.Message<T> originalMessage = originalMessagesById.get(entry.id());
		Assert.notNull(originalMessage,
				() -> "Could not correlate send result to original message for id %s. Original messages: %s."
						.formatted(entry.messageId(), originalMessagesById));
		return originalMessage;
	}

	@Nullable
	@Override
	protected <T> MessageConversionContext getReceiveMessageConversionContext(String endpointName,
			@Nullable Class<T> payloadClass) {
		return this.conversionContextCache.computeIfAbsent(endpointName,
				newEndpoint -> doGetSqsMessageConversionContext(endpointName, payloadClass));
	}

	private <T> SqsMessageConversionContext doGetSqsMessageConversionContext(String newEndpoint,
			@Nullable Class<T> payloadClass) {
		SqsMessageConversionContext conversionContext = new SqsMessageConversionContext();
		conversionContext.setSqsAsyncClient(this.sqsAsyncClient);
		// At this point we'll already have retrieved and cached the queue attributes
		conversionContext.setQueueAttributes(getAttributesImmediately(newEndpoint));
		if (payloadClass != null) {
			conversionContext.setPayloadClass(payloadClass);
		}
		conversionContext.setAcknowledgementCallback(new TemplateAcknowledgementCallback<T>());
		return conversionContext;
	}

	private QueueAttributes getAttributesImmediately(String newEndpoint) {
		CompletableFuture<QueueAttributes> queueAttributes = getQueueAttributes(newEndpoint);
		Assert.isTrue(queueAttributes.isDone(), () -> "Queue attributes not done for " + newEndpoint);
		return queueAttributes.join();
	}

	private CompletableFuture<SendMessageBatchRequest> createSendMessageBatchRequest(String endpointName,
			Collection<Message> messages) {
		return getQueueAttributes(endpointName)
				.thenApply(queueAttributes -> doCreateSendMessageBatchRequest(messages, queueAttributes));
	}

	private SendMessageBatchRequest doCreateSendMessageBatchRequest(Collection<Message> messages,
			QueueAttributes queueAttributes) {
		return SendMessageBatchRequest.builder().queueUrl(queueAttributes.getQueueUrl())
				.entries(messages.stream().map(this::createSendMessageBatchRequestEntry).collect(Collectors.toList()))
				.build();
	}

	private SendMessageBatchRequestEntry createSendMessageBatchRequestEntry(Message message) {
		return SendMessageBatchRequestEntry.builder().id(message.messageId()).messageBody(message.body())
				.messageDeduplicationId(message.attributes().get(MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID))
				.messageGroupId(message.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID))
				.delaySeconds(getDelaySeconds(message))
				.messageAttributes(excludeKnownFields(message.messageAttributes()))
				.messageSystemAttributes(mapMessageSystemAttributes(message)).build();
	}

	private Map<String, MessageAttributeValue> excludeKnownFields(
			Map<String, MessageAttributeValue> messageAttributes) {
		return messageAttributes.entrySet().stream()
				.filter(entry -> !SqsHeaders.SQS_DELAY_HEADER.equals(entry.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Nullable
	private Integer getDelaySeconds(Message message) {
		return message.messageAttributes().containsKey(SqsHeaders.SQS_DELAY_HEADER)
				? Integer.parseInt(message.messageAttributes().get(SqsHeaders.SQS_DELAY_HEADER).stringValue())
				: null;
	}

	private Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue> mapMessageSystemAttributes(
			Message message) {
		return message.attributes().entrySet().stream().filter(Predicate.not(entry -> isSkipAttribute(entry.getKey())))
				.collect(Collectors
						.toMap(entry -> MessageSystemAttributeNameForSends.fromValue(entry.getKey().toString()),
								entry -> MessageSystemAttributeValue.builder()
										.dataType(MessageAttributeDataTypes.STRING).stringValue(entry.getValue())
										.build()));
	}

	private boolean isSkipAttribute(MessageSystemAttributeName name) {
		return MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID.equals(name)
				|| MessageSystemAttributeName.MESSAGE_GROUP_ID.equals(name);
	}

	private CompletableFuture<QueueAttributes> getQueueAttributes(String endpointName) {
		return this.queueAttributesCache.computeIfAbsent(endpointName,
				newName -> doGetQueueAttributes(endpointName, newName));
	}

	private CompletableFuture<QueueAttributes> doGetQueueAttributes(String endpointName, String newName) {
		return QueueAttributesResolver.builder().sqsAsyncClient(this.sqsAsyncClient).queueName(newName)
				.queueNotFoundStrategy(this.queueNotFoundStrategy)
				.queueAttributeNames(maybeAddContentBasedDeduplicationAttribute(endpointName)).build()
				.resolveQueueAttributes();
	}

	private Collection<QueueAttributeName> maybeAddContentBasedDeduplicationAttribute(String endpointName) {
		return FifoUtils.isFifo(endpointName)
				&& TemplateContentBasedDeduplication.AUTO.equals(this.contentBasedDeduplication)
						? Stream.concat(queueAttributeNames.stream(),
								Stream.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION)).toList()
						: queueAttributeNames;
	}

	@Override
	protected Map<String, Object> handleAdditionalHeaders(Map<String, Object> additionalHeaders) {
		HashMap<String, Object> headers = new HashMap<>(additionalHeaders);
		headers.remove(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER);
		headers.remove(SqsHeaders.SQS_RECEIVE_REQUEST_ATTEMPT_ID_HEADER);
		return headers;
	}

	@Override
	protected CompletableFuture<Void> doAcknowledgeMessages(String endpointName,
			Collection<org.springframework.messaging.Message<?>> messages) {
		return deleteMessages(endpointName, messages);
	}

	@Override
	protected CompletableFuture<Collection<Message>> doReceiveAsync(String endpointName, Duration pollTimeout,
			Integer maxNumberOfMessages, Map<String, Object> additionalHeaders) {
		logger.trace(
				"Receiving messages with settings: endpointName - {}, pollTimeout - {}, maxNumberOfMessages - {}, additionalHeaders - {}",
				endpointName, pollTimeout, maxNumberOfMessages, additionalHeaders);
		return createReceiveMessageRequest(endpointName, pollTimeout, maxNumberOfMessages, additionalHeaders)
				.thenCompose(this.sqsAsyncClient::receiveMessage).thenApply(ReceiveMessageResponse::messages);
	}

	@Override
	protected Map<String, Object> preProcessHeadersForReceive(String endpointToUse, Map<String, Object> headers) {
		return FifoUtils.isFifo(endpointToUse) ? addMissingFifoReceiveHeaders(headers) : headers;
	}

	private Map<String, Object> addMissingFifoReceiveHeaders(Map<String, Object> headers) {
		headers.putIfAbsent(SqsHeaders.SQS_RECEIVE_REQUEST_ATTEMPT_ID_HEADER, UUID.randomUUID());
		return headers;
	}

	private CompletableFuture<Void> deleteMessages(String endpointName,
			Collection<org.springframework.messaging.Message<?>> messages) {
		logger.trace("Acknowledging in queue {} messages {}", endpointName,
				MessageHeaderUtils.getId(addTypeToMessages(messages)));
		return getQueueAttributes(endpointName)
				.thenCompose(attributes -> this.sqsAsyncClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
						.queueUrl(attributes.getQueueUrl()).entries(createDeleteMessageEntries(messages)).build()))
				.exceptionallyCompose(
						t -> createAcknowledgementException(endpointName, Collections.emptyList(), messages, t))
				.thenCompose(response -> !response.failed().isEmpty()
						? createAcknowledgementException(endpointName,
								getSuccessfulAckMessages(response, messages, endpointName),
								getFailedAckMessages(response, messages, endpointName), null)
						: CompletableFuture.completedFuture(response))
				.whenComplete((response, t) -> logAcknowledgement(endpointName, messages, response, t)).thenRun(() -> {
				});
	}

	private Collection<org.springframework.messaging.Message<?>> getFailedAckMessages(
			DeleteMessageBatchResponse response, Collection<org.springframework.messaging.Message<?>> messages,
			String endpointName) {
		return response.failed().stream().map(BatchResultErrorEntry::id)
				.map(id -> messages.stream().filter(msg -> MessageHeaderUtils.getId(msg).equals(id)).findFirst()
						.orElseThrow(() -> new SqsAcknowledgementException(
								"Could not correlate ids for acknowledgement failure", Collections.emptyList(),
								messages, endpointName)))
				.collect(Collectors.toList());
	}

	private Collection<org.springframework.messaging.Message<?>> getSuccessfulAckMessages(
			DeleteMessageBatchResponse response, Collection<org.springframework.messaging.Message<?>> messages,
			String endpointName) {
		return response.successful().stream().map(DeleteMessageBatchResultEntry::id)
				.map(id -> messages.stream().filter(msg -> MessageHeaderUtils.getId(msg).equals(id)).findFirst()
						.orElseThrow(() -> new SqsAcknowledgementException(
								"Could not correlate ids for acknowledgement failure", Collections.emptyList(),
								messages, endpointName)))
				.collect(Collectors.toList());
	}

	private CompletableFuture<DeleteMessageBatchResponse> createAcknowledgementException(String endpointName,
			Collection<org.springframework.messaging.Message<?>> successfulAckMessages,
			Collection<org.springframework.messaging.Message<?>> failedAckMessages, @Nullable Throwable t) {
		return CompletableFuture.failedFuture(new SqsAcknowledgementException("Error acknowledging messages",
				successfulAckMessages, failedAckMessages, endpointName, t));
	}

	private void logAcknowledgement(String endpointName, Collection<org.springframework.messaging.Message<?>> messages,
			DeleteMessageBatchResponse response, @Nullable Throwable t) {
		if (t != null) {
			logger.error("Error acknowledging in queue {} messages {}", endpointName,
					MessageHeaderUtils.getId(addTypeToMessages(messages)));
		}
		else if (!response.failed().isEmpty()) {
			logger.warn("Some messages could not be acknowledged in queue {}: {}", endpointName,
					response.failed().stream().map(BatchResultErrorEntry::id).toList());
		}
		else {
			logger.trace("Acknowledged messages in queue {}: {}", endpointName,
					MessageHeaderUtils.getId(addTypeToMessages(messages)));
		}
	}

	private Collection<DeleteMessageBatchRequestEntry> createDeleteMessageEntries(
			Collection<org.springframework.messaging.Message<?>> messages) {
		return messages.stream()
				.map(message -> DeleteMessageBatchRequestEntry.builder().id(MessageHeaderUtils.getId(message))
						.receiptHandle(
								MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.SQS_RECEIPT_HANDLE_HEADER))
						.build())
				.collect(Collectors.toList());
	}

	private CompletableFuture<ReceiveMessageRequest> createReceiveMessageRequest(String endpointName,
			Duration pollTimeout, Integer maxNumberOfMessages, Map<String, Object> additionalHeaders) {
		return getQueueAttributes(endpointName).thenApply(attributes -> doCreateReceiveMessageRequest(pollTimeout,
				maxNumberOfMessages, attributes, additionalHeaders));
	}

	private ReceiveMessageRequest doCreateReceiveMessageRequest(Duration pollTimeout, Integer maxNumberOfMessages,
			QueueAttributes attributes, Map<String, Object> additionalHeaders) {
		ReceiveMessageRequest.Builder builder = ReceiveMessageRequest.builder().queueUrl(attributes.getQueueUrl())
				.maxNumberOfMessages(maxNumberOfMessages).messageAttributeNames(this.messageAttributeNames)
				.attributeNamesWithStrings(this.messageSystemAttributeNames)
				.waitTimeSeconds(toInt(pollTimeout.toSeconds()));
		if (additionalHeaders.containsKey(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER)) {
			builder.visibilityTimeout(
					toInt(getValueAs(additionalHeaders, SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Duration.class)
							.toSeconds()));
		}
		if (additionalHeaders.containsKey(SqsHeaders.SQS_RECEIVE_REQUEST_ATTEMPT_ID_HEADER)) {
			builder.receiveRequestAttemptId(
					getValueAs(additionalHeaders, SqsHeaders.SQS_RECEIVE_REQUEST_ATTEMPT_ID_HEADER, UUID.class)
							.toString());
		}
		return builder.build();
	}

	// Convert a long value to an int. Values larger than Integer.MAX_VALUE are set to Integer.MAX_VALUE
	private int toInt(long longValue) {
		if (longValue > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return (int) longValue;
	}

	private <V> V getValueAs(Map<String, Object> headers, String headerName, Class<V> valueClass) {
		return valueClass.cast(headers.get(headerName));
	}

	private static SqsMessagingMessageConverter createDefaultMessageConverter() {
		return new SqsMessagingMessageConverter();
	}

	private static class SqsTemplateOptionsImpl extends AbstractMessagingTemplateOptions<SqsTemplateOptions>
			implements SqsTemplateOptions {

		private Collection<QueueAttributeName> queueAttributeNames = Collections.emptyList();

		private QueueNotFoundStrategy queueNotFoundStrategy = QueueNotFoundStrategy.CREATE;

		private Collection<String> messageAttributeNames = Collections.singletonList("All");

		private Collection<String> messageSystemAttributeNames = Collections.singletonList("All");

		private TemplateContentBasedDeduplication contentBasedDeduplication = TemplateContentBasedDeduplication.AUTO;

		@Override
		public SqsTemplateOptions queueAttributeNames(Collection<QueueAttributeName> queueAttributeNames) {
			Assert.notEmpty(queueAttributeNames, "queueAttributeNames cannot be null or empty");
			this.queueAttributeNames = queueAttributeNames;
			return this;
		}

		@Override
		public SqsTemplateOptions defaultQueue(String defaultQueue) {
			super.defaultEndpointName(defaultQueue);
			return this;
		}

		@Override
		public SqsTemplateOptions queueNotFoundStrategy(QueueNotFoundStrategy queueNotFoundStrategy) {
			Assert.notNull(queueNotFoundStrategy, "queueNotFoundStrategy cannot be null");
			this.queueNotFoundStrategy = queueNotFoundStrategy;
			return this;
		}

		@Override
		public SqsTemplateOptions messageAttributeNames(Collection<String> messageAttributeNames) {
			this.messageAttributeNames = messageAttributeNames;
			return this;
		}

		@Override
		public SqsTemplateOptions messageSystemAttributeNames(
				Collection<MessageSystemAttributeName> messageSystemAttributeNames) {
			this.messageSystemAttributeNames = messageSystemAttributeNames.stream()
					.map(MessageSystemAttributeName::name).toList();
			return this;
		}

		@Override
		public SqsTemplateOptions contentBasedDeduplication(
				TemplateContentBasedDeduplication contentBasedDeduplication) {
			this.contentBasedDeduplication = contentBasedDeduplication;
			return this;
		}

		@Override
		public SqsTemplateOptions observationConvention(SqsTemplateObservation.Convention observationConvention) {
			Assert.notNull(observationConvention, "observationConvention cannot be null");
			super.observationConvention(observationConvention);
			return this;
		}

	}

	private static class SqsTemplateBuilderImpl implements SqsTemplateBuilder {

		private final SqsTemplateOptionsImpl options;

		private SqsAsyncClient sqsAsyncClient;

		private MessagingMessageConverter<Message> messageConverter;

		private SqsTemplateBuilderImpl() {
			this.options = new SqsTemplateOptionsImpl();
		}

		@Override
		public SqsTemplateBuilder sqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
			Assert.notNull(sqsAsyncClient, "sqsAsyncClient must not be null");
			this.sqsAsyncClient = sqsAsyncClient;
			return this;
		}

		@Override
		public SqsTemplateBuilder messageConverter(MessagingMessageConverter<Message> messageConverter) {
			Assert.notNull(messageConverter, "messageConverter must not be null");
			Assert.isNull(this.messageConverter, "messageConverter already configured");
			this.messageConverter = messageConverter;
			return this;
		}

		@Override
		public SqsTemplateBuilder configureDefaultConverter(
				Consumer<SqsMessagingMessageConverter> messageConverterConfigurer) {
			Assert.notNull(messageConverterConfigurer, "messageConverterConfigurer must not be null");
			Assert.isNull(this.messageConverter, "messageConverter already configured");
			SqsMessagingMessageConverter defaultMessageConverter = createDefaultMessageConverter();
			messageConverterConfigurer.accept(defaultMessageConverter);
			this.messageConverter = defaultMessageConverter;
			return this;
		}

		@Override
		public SqsTemplateBuilder configure(Consumer<SqsTemplateOptions> options) {
			Assert.notNull(options, "options must not be null");
			options.accept(this.options);
			return this;
		}

		@Override
		public SqsTemplate build() {
			Assert.notNull(this.sqsAsyncClient, "no sqsAsyncClient set");
			if (this.messageConverter == null) {
				this.messageConverter = createDefaultMessageConverter();
			}
			return new SqsTemplate(this);
		}

		@Override
		public SqsOperations buildSyncTemplate() {
			return build();
		}

		@Override
		public SqsAsyncOperations buildAsyncTemplate() {
			return build();
		}

	}

	private static class SqsSendOptionsImpl<T> implements SqsSendOptions<T> {

		protected final Map<String, Object> headers = new HashMap<>();

		@Nullable
		private String messageGroupId;

		@Nullable
		private String messageDeduplicationId;

		@Nullable
		protected String queue;

		@Nullable
		protected T payload;

		@Nullable
		protected Integer delay;

		@Override
		public SqsSendOptionsImpl<T> queue(String queue) {
			Assert.hasText(queue, "queue must have text");
			this.queue = queue;
			return this;
		}

		@Override
		public SqsSendOptionsImpl<T> payload(T payload) {
			Assert.notNull(payload, "payload must not be null");
			this.payload = payload;
			return this;
		}

		@Override
		public SqsSendOptionsImpl<T> header(String headerName, Object headerValue) {
			Assert.hasText(headerName, "headerName must have text");
			Assert.notNull(headerValue, "headerValue must not be null");
			this.headers.put(headerName, headerValue);
			return this;
		}

		@Override
		public SqsSendOptionsImpl<T> headers(Map<String, Object> headers) {
			Assert.notNull(headers, "headers must not be null");
			this.headers.putAll(headers);
			return this;
		}

		@Override
		public SqsSendOptionsImpl<T> delaySeconds(Integer delaySeconds) {
			Assert.notNull(delaySeconds, "delaySeconds must not be null");
			this.delay = delaySeconds;
			return this;
		}

		@Override
		public SqsSendOptions<T> messageGroupId(String messageGroupId) {
			Assert.hasText(messageGroupId, "messageGroupId must have text");
			this.messageGroupId = messageGroupId;
			return this;
		}

		@Override
		public SqsSendOptions<T> messageDeduplicationId(String messageDeduplicationId) {
			Assert.hasText(messageDeduplicationId, "messageDeduplicationId must have text");
			this.messageDeduplicationId = messageDeduplicationId;
			return this;
		}

	}

	private static class SqsReceiveOptionsImpl implements SqsReceiveOptions {

		protected final Map<String, Object> additionalHeaders = new HashMap<>();

		@Nullable
		protected String queue;

		@Nullable
		protected Duration pollTimeout;

		@Nullable
		protected Duration visibilityTimeout;

		@Nullable
		protected Integer maxNumberOfMessages;

		@Nullable
		private UUID receiveRequestAttemptId;

		@Override
		public SqsReceiveOptionsImpl queue(String queue) {
			Assert.notNull(queue, "queue must not be null");
			this.queue = queue;
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl pollTimeout(Duration pollTimeout) {
			Assert.notNull(pollTimeout, "pollTimeout must not be null");
			this.pollTimeout = pollTimeout;
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl visibilityTimeout(Duration visibilityTimeout) {
			Assert.notNull(visibilityTimeout, "visibilityTimeout must not be null");
			this.visibilityTimeout = visibilityTimeout;
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl maxNumberOfMessages(Integer maxNumberOfMessages) {
			Assert.notNull(maxNumberOfMessages, "maxNumberOfMessages must not be null");
			Assert.isTrue(maxNumberOfMessages > 0 && maxNumberOfMessages <= 10,
					"maxNumberOfMessages must be between 0 and 10");
			this.maxNumberOfMessages = maxNumberOfMessages;
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl additionalHeader(String name, Object value) {
			Assert.notNull(name, "name must not be null");
			Assert.notNull(value, "value must not be null");
			this.additionalHeaders.put(name, value);
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl additionalHeaders(Map<String, Object> additionalHeaders) {
			Assert.notNull(additionalHeaders, "additionalHeaders must not be null");
			this.additionalHeaders.putAll(additionalHeaders);
			return this;
		}

		@Override
		public SqsReceiveOptionsImpl receiveRequestAttemptId(UUID receiveRequestAttemptId) {
			Assert.notNull(receiveRequestAttemptId, "receiveRequestAttemptId must not be null");
			this.receiveRequestAttemptId = receiveRequestAttemptId;
			return this;
		}

	}

	private class TemplateAcknowledgementCallback<T> implements AcknowledgementCallback<T> {

		@Override
		public CompletableFuture<Void> onAcknowledge(org.springframework.messaging.Message<T> message) {
			return deleteMessages(MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.SQS_QUEUE_NAME_HEADER),
					Collections.singletonList(message));
		}

		@Override
		public CompletableFuture<Void> onAcknowledge(Collection<org.springframework.messaging.Message<T>> messages) {
			return messages.isEmpty() ? CompletableFuture.completedFuture(null)
					: deleteMessages(
							MessageHeaderUtils.getHeaderAsString(messages.iterator().next(),
									SqsHeaders.SQS_QUEUE_NAME_HEADER),
							messages.stream().map(msg -> (org.springframework.messaging.Message<?>) msg)
									.collect(Collectors.toList()));
		}
	}

}
