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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.support.converter.ContextAwareMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessagingOperations}
 * @param <T> the payload type
 * @param <S> the source message type for conversion
 */
public abstract class AbstractMessagingTemplate<T, S> implements MessagingOperations<T>, AsyncMessagingOperations<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingTemplate.class);

	private static final TemplateAcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = TemplateAcknowledgementMode.ACKNOWLEDGE;

	private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

	private static final SendBatchFailureHandlingStrategy DEFAULT_SEND_BATCH_OPERATION_FAILURE_STRATEGY = SendBatchFailureHandlingStrategy.THROW;

	private static final int DEFAULT_MAX_NUMBER_OF_MESSAGES = 10;

	private static final String DEFAULT_ENDPOINT_NAME = "";

	private final Map<String, Object> defaultAdditionalHeaders;

	private final Duration defaultPollTimeout;

	private final int defaultMaxNumberOfMessages;

	private final String defaultEndpointName;

	private final TemplateAcknowledgementMode acknowledgementMode;

	private final SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy;

	@Nullable
	private final Class<T> defaultPayloadClass;

	private final MessagingMessageConverter<S> messageConverter;

	protected AbstractMessagingTemplate(MessagingMessageConverter<S> messageConverter,
			AbstractMessagingTemplateOptions<T, ?> options) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		Assert.notNull(options, "options must not be null");
		this.messageConverter = messageConverter;
		this.defaultAdditionalHeaders = options.defaultAdditionalHeaders;
		this.defaultMaxNumberOfMessages = options.defaultMaxNumberOfMessages;
		this.defaultPollTimeout = options.defaultPollTimeout;
		this.defaultPayloadClass = options.defaultPayloadClass;
		this.defaultEndpointName = options.defaultEndpointName;
		this.acknowledgementMode = options.acknowledgementMode;
		this.sendBatchFailureHandlingStrategy = options.sendBatchFailureHandlingStrategy;
	}

	@Override
	public Optional<Message<T>> receive() {
		return unwrapCompletionException(receiveAsync());
	}

	@Override
	public Optional<Message<T>> receive(@Nullable String endpoint, @Nullable Class<T> payloadClass,
			@Nullable Duration pollTimeout, @Nullable Map<String, Object> additionalHeaders) {
		return unwrapCompletionException(receiveAsync(endpoint, payloadClass, pollTimeout, additionalHeaders));
	}

	@Override
	public Collection<Message<T>> receiveMany() {
		return unwrapCompletionException(receiveManyAsync());
	}

	@Override
	public Collection<Message<T>> receiveMany(@Nullable String endpoint, @Nullable Class<T> payloadClass,
			@Nullable Duration pollTimeout, @Nullable Integer maxNumberOfMessages,
			@Nullable Map<String, Object> additionalHeaders) {
		return unwrapCompletionException(
				receiveManyAsync(endpoint, payloadClass, pollTimeout, maxNumberOfMessages, additionalHeaders));
	}

	@Override
	public CompletableFuture<Optional<Message<T>>> receiveAsync() {
		return receiveAsync(null, null, null, null);
	}

	@Override
	public CompletableFuture<Optional<Message<T>>> receiveAsync(@Nullable String endpoint,
			@Nullable Class<? extends T> payloadClass, @Nullable Duration pollTimeout,
			@Nullable Map<String, Object> additionalHeaders) {
		return receiveManyAsync(endpoint, payloadClass, pollTimeout, 1, additionalHeaders)
				.thenApply(msgs -> msgs.isEmpty() ? Optional.empty() : Optional.of(msgs.iterator().next()));
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> receiveManyAsync() {
		return receiveManyAsync(null, null, null, null, null);
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> receiveManyAsync(@Nullable String endpoint,
			@Nullable Class<? extends T> payloadClass, @Nullable Duration pollTimeout, @Nullable Integer maxNumberOfMessages,
			@Nullable Map<String, Object> additionalHeaders) {
		String endpointToUse = getEndpointName(endpoint);
		logger.trace("Receiving messages from endpoint {}", endpointToUse);
		Map<String, Object> headers = getAdditionalHeadersToReceive(additionalHeaders);
		return doReceiveAsync(endpointToUse, getOrDefault(pollTimeout, this.defaultPollTimeout, "pollTimeout"),
				getOrDefault(maxNumberOfMessages, this.defaultMaxNumberOfMessages, "defaultMaxNumberOfMessages"),
				headers).thenApply(messages -> convertReceivedMessages(endpointToUse, payloadClass, messages, headers))
						.thenCompose(messages -> handleAcknowledgement(endpointToUse, messages))
						.exceptionallyCompose(t -> CompletableFuture.failedFuture(new MessagingOperationFailedException(
								"Message receive operation failed for endpoint %s".formatted(endpointToUse),
								endpointToUse, t instanceof CompletionException ? t.getCause() : t)))
						.whenComplete((v, t) -> logReceiveMessageResult(endpointToUse, v, t));
	}

	private Map<String, Object> getAdditionalHeadersToReceive(@Nullable Map<String, Object> additionalHeaders) {
		Map<String, Object> headers = new HashMap<>(this.defaultAdditionalHeaders);
		if (additionalHeaders != null) {
			headers.putAll(additionalHeaders);
		}
		return headers;
	}

	private Collection<Message<T>> convertReceivedMessages(String endpoint, @Nullable Class<? extends T> payloadClass,
			Collection<S> messages, Map<String, Object> additionalHeaders) {
		return messages.stream()
				.map(message -> convertReceivedMessage(getEndpointName(endpoint), message,
						payloadClass != null ? payloadClass : this.defaultPayloadClass))
				.map(message -> addAdditionalHeaders(message, additionalHeaders)).collect(Collectors.toList());
	}

	protected Message<T> addAdditionalHeaders(Message<T> message, Map<String, Object> additionalHeaders) {
		Map<String, Object> headersToAdd = handleAdditionalHeaders(additionalHeaders);
		return headersToAdd.isEmpty() ? message : MessageHeaderUtils.addHeadersToMessage(message, headersToAdd);
	}

	protected abstract Map<String, Object> handleAdditionalHeaders(Map<String, Object> additionalHeaders);

	private CompletableFuture<Collection<Message<T>>> handleAcknowledgement(@Nullable String endpoint,
			Collection<Message<T>> messages) {
		return TemplateAcknowledgementMode.ACKNOWLEDGE.equals(this.acknowledgementMode) && !messages.isEmpty()
				? doAcknowledgeMessages(getEndpointName(endpoint), messages).thenApply(theVoid -> messages)
				: CompletableFuture.completedFuture(messages);
	}

	protected abstract CompletableFuture<Void> doAcknowledgeMessages(String endpointName,
			Collection<Message<T>> messages);

	private String getEndpointName(@Nullable String endpoint) {
		String endpointName = getOrDefault(endpoint, this.defaultEndpointName, "endpointName");
		Assert.hasText(endpointName, "No endpoint name informed and no default value available");
		return endpointName;
	}

	private <V> V getOrDefault(@Nullable V value, V defaultValue, String valueName) {
		return Objects.requireNonNull(value != null ? value : defaultValue,
				valueName + " not set and no default value provided");
	}

	@SuppressWarnings("unchecked")
	private Message<T> convertReceivedMessage(String endpoint, S message, @Nullable Class<? extends T> payloadClass) {
		return this.messageConverter instanceof ContextAwareMessagingMessageConverter<S> contextConverter
				? (Message<T>) contextConverter.toMessagingMessage(message,
						getReceiveMessageConversionContext(endpoint, payloadClass))
				: (Message<T>) this.messageConverter.toMessagingMessage(message);
	}

	private void logReceiveMessageResult(String endpoint, @Nullable Collection<Message<T>> v, @Nullable Throwable t) {
		if (v != null) {
			logger.trace("Received messages {} from endpoint {}", MessageHeaderUtils.getId(v), endpoint);
		}
		else {
			logger.error("Error receiving messages", t);
		}
	}

	protected abstract CompletableFuture<Collection<S>> doReceiveAsync(String endpointName, Duration pollTimeout,
			Integer maxNumberOfMessages, Map<String, Object> additionalHEaders);

	@Override
	public SendResult<T> send(T payload) {
		return unwrapCompletionException(sendAsync(payload));
	}

	@Override
	public SendResult<T> send(@Nullable String endpointName, T payload) {
		return unwrapCompletionException(sendAsync(endpointName, payload));
	}

	@Override
	public SendResult<T> send(@Nullable String endpointName, Message<T> message) {
		return unwrapCompletionException(sendAsync(endpointName, message));
	}

	@Override
	public SendResult.Batch<T> sendMany(@Nullable String endpointName, Collection<Message<T>> messages) {
		return unwrapCompletionException(sendManyAsync(endpointName, messages));
	}

	@Override
	public CompletableFuture<SendResult<T>> sendAsync(T payload) {
		return sendAsync(null, payload);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CompletableFuture<SendResult<T>> sendAsync(@Nullable String endpointName, T payload) {
		return sendAsync(endpointName,
				payload instanceof Message ? (Message<T>) payload : MessageBuilder.withPayload(payload).build());
	}

	@Override
	public CompletableFuture<SendResult<T>> sendAsync(@Nullable String endpointName, Message<T> message) {
		String endpointToUse = getEndpointName(endpointName);
		logger.trace("Sending message {} to endpoint {}", MessageHeaderUtils.getId(message), endpointName);
		return doSendAsync(endpointToUse, convertMessageToSend(message), message)
				.exceptionallyCompose(
						t -> CompletableFuture
								.failedFuture(new MessagingOperationFailedException(
										"Message send operation failed for message %s to endpoint %s"
												.formatted(MessageHeaderUtils.getId(message), endpointToUse),
										endpointToUse, message, t)))
				.whenComplete((v, t) -> logSendMessageResult(endpointToUse, message, t));
	}

	@Override
	public CompletableFuture<SendResult.Batch<T>> sendManyAsync(@Nullable String endpointName,
			Collection<Message<T>> messages) {
		logger.trace("Sending messages {} to endpoint {}", MessageHeaderUtils.getId(messages), endpointName);
		String endpointToUse = getEndpointName(endpointName);
		return doSendBatchAsync(endpointToUse, convertMessagesToSend(messages), messages)
				.exceptionallyCompose(t -> wrapSendException(messages, endpointToUse, t))
				.thenCompose(result -> handleFailedMessages(endpointToUse, result))
				.whenComplete((v, t) -> logSendMessageBatchResult(endpointToUse, messages, t));
	}

	private CompletableFuture<SendResult.Batch<T>> handleFailedMessages(String endpointToUse,
			SendResult.Batch<T> result) {
		return !result.failed().isEmpty()
				&& SendBatchFailureHandlingStrategy.THROW.equals(this.sendBatchFailureHandlingStrategy)
						? handleFailedSendBatch(endpointToUse, result)
						: CompletableFuture.completedFuture(result);
	}

	private CompletableFuture<SendResult.Batch<T>> wrapSendException(Collection<Message<T>> messages,
			String endpointToUse, Throwable t) {
		return CompletableFuture.failedFuture(
				new MessagingOperationFailedException("Message send operation failed for messages %s to endpoint %s"
						.formatted(MessageHeaderUtils.getId(messages), endpointToUse), endpointToUse, messages, t));
	}

	private CompletableFuture<SendResult.Batch<T>> handleFailedSendBatch(String endpoint, SendResult.Batch<T> result) {
		return CompletableFuture.failedFuture(new SendBatchOperationFailedException("", endpoint, result));
	}

	private Collection<S> convertMessagesToSend(Collection<Message<T>> messages) {
		return messages.stream().map(this::convertMessageToSend).collect(Collectors.toList());
	}

	private S convertMessageToSend(Message<T> message) {
		return this.messageConverter instanceof ContextAwareMessagingMessageConverter<S> contextConverter
				? contextConverter.fromMessagingMessage(message, getSendMessageConversionContext(message))
				: messageConverter.fromMessagingMessage(message);
	}

	protected abstract CompletableFuture<SendResult<T>> doSendAsync(String endpointName, S message,
			Message<T> originalMessage);

	protected abstract CompletableFuture<SendResult.Batch<T>> doSendBatchAsync(String endpointName,
			Collection<S> messages, Collection<Message<T>> originalMessages);

	@Nullable
	protected MessageConversionContext getReceiveMessageConversionContext(String endpointName,
			@Nullable Class<? extends T> payloadClass) {
		// Subclasses can override this method to return a context
		return null;
	}

	@Nullable
	protected MessageConversionContext getSendMessageConversionContext(Message<T> message) {
		// Subclasses can override this method to return a context
		return null;
	}

	private void logSendMessageResult(String endpointToUse, Message<T> message, @Nullable Throwable t) {
		if (t == null) {
			logger.trace("Message {} successfully sent to endpoint {} with id {}", message,
					MessageHeaderUtils.getId(message), endpointToUse);
		}
		else {
			logger.error("Error sending message {} to endpoint {}", MessageHeaderUtils.getId(message), endpointToUse,
					unwrapCompletionException(t));
		}
	}

	private Throwable unwrapCompletionException(Throwable t) {
		return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
	}

	private void logSendMessageBatchResult(String endpointToUse, Collection<Message<T>> messages,
			@Nullable Throwable t) {
		if (t == null) {
			logger.trace("Messages {} successfully sent to endpoint {} with id {}", messages,
					MessageHeaderUtils.getId(messages), endpointToUse);
		}
		else {
			logger.error("Error sending messages {} to endpoint {}", MessageHeaderUtils.getId(messages), endpointToUse,
					unwrapCompletionException(t));
		}
	}

	protected <V> V unwrapCompletionException(CompletableFuture<V> future) {
		try {
			return future.join();
		}
		catch (CompletionException ex) {
			if (ex.getCause()instanceof RuntimeException re) {
				throw re;
			}
			throw new RuntimeException("Unexpected exception", ex);
		}
	}

	/**
	 * Options to be used by the template.
	 * @param <T> the payload type.
	 * @param <O> the options subclass to be returned by the chained methods.
	 */
	protected interface MessagingTemplateOptions<T, O extends MessagingTemplateOptions<T, O>> {

		/**
		 * Set the acknowledgement mode for this template. Default is {@link TemplateAcknowledgementMode#ACKNOWLEDGE}
		 * @param acknowledgementMode the mode.
		 * @return the options instance.
		 */
		O acknowledgementMode(TemplateAcknowledgementMode acknowledgementMode);

		/**
		 * Set the strategy to use when handling batch send operations with at least one failed message. Default is
		 * {@link SendBatchFailureHandlingStrategy#THROW}
		 * @param sendBatchFailureHandlingStrategy the strategy.
		 * @return the options instance.
		 */
		O sendBatchFailureHandlingStrategy(SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy);

		/**
		 * Set the default maximum amount of time this template will wait for the maximum number of messages before
		 * returning. Default is 10 seconds.
		 * @param defaultPollTimeout the timeout.
		 * @return the options instance.
		 */
		O defaultPollTimeout(Duration defaultPollTimeout);

		/**
		 * Set the default maximum number of messages to be retrieved in a single batch. Default is 10.
		 * @param defaultMaxNumberOfMessages the maximum number of messages.
		 * @return the options instance.
		 */
		O defaultMaxNumberOfMessages(Integer defaultMaxNumberOfMessages);

		/**
		 * Set the default endpoint name for this template. Default is blank.
		 * @param defaultEndpointName the default endpoint name.
		 * @return the options instance.
		 */
		O defaultEndpointName(String defaultEndpointName);

		/**
		 * The default class to which this template should convert payloads to.
		 * @param defaultPayloadClass the default payload class.
		 * @return the options instance.
		 */
		O defaultPayloadClass(Class<T> defaultPayloadClass);

		/**
		 * Set a default header to be added to received messages.
		 * @param name the header name.
		 * @param value the header value.
		 * @return the options instance.
		 */
		O additionalHeaderForReceive(String name, Object value);

		/**
		 * Set default headers to be added to received messages.
		 * @param defaultAdditionalHeaders the headers.
		 * @return the options instance.
		 */
		O additionalHeadersForReceive(Map<String, Object> defaultAdditionalHeaders);

	}

	/**
	 * Base class for template options, to be extended by subclasses.
	 * @param <T> the payload type
	 * @param <O> the options type for returning in the chained methods.
	 */
	protected static abstract class AbstractMessagingTemplateOptions<T, O extends MessagingTemplateOptions<T, O>>
			implements MessagingTemplateOptions<T, O> {

		private Duration defaultPollTimeout = DEFAULT_POLL_TIMEOUT;

		private int defaultMaxNumberOfMessages = DEFAULT_MAX_NUMBER_OF_MESSAGES;

		private String defaultEndpointName = DEFAULT_ENDPOINT_NAME;

		private TemplateAcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

		private SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy = DEFAULT_SEND_BATCH_OPERATION_FAILURE_STRATEGY;

		private final Map<String, Object> defaultAdditionalHeaders = new HashMap<>();

		@Nullable
		private Class<T> defaultPayloadClass;

		@Override
		public O acknowledgementMode(TemplateAcknowledgementMode defaultAcknowledgementMode) {
			Assert.notNull(defaultAcknowledgementMode, "defaultAcknowledgementMode must not be null");
			this.acknowledgementMode = defaultAcknowledgementMode;
			return self();
		}

		@Override
		public O sendBatchFailureHandlingStrategy(SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy) {
			Assert.notNull(sendBatchFailureHandlingStrategy, "sendBatchFailureStrategy must not be null");
			this.sendBatchFailureHandlingStrategy = sendBatchFailureHandlingStrategy;
			return self();
		}

		@Override
		public O defaultPollTimeout(Duration defaultPollTimeout) {
			Assert.notNull(defaultPollTimeout, "pollTimeout must not be null");
			this.defaultPollTimeout = defaultPollTimeout;
			return self();
		}

		@Override
		public O defaultMaxNumberOfMessages(Integer defaultMaxNumberOfMessages) {
			Assert.isTrue(defaultMaxNumberOfMessages > 0, "defaultMaxNumberOfMessages must be greater than zero");
			this.defaultMaxNumberOfMessages = defaultMaxNumberOfMessages;
			return self();
		}

		@Override
		public O defaultEndpointName(String defaultEndpointName) {
			Assert.hasText(defaultEndpointName, "defaultEndpointName must have text");
			this.defaultEndpointName = defaultEndpointName;
			return self();
		}

		@Override
		public O defaultPayloadClass(Class<T> defaultPayloadClass) {
			Assert.notNull(defaultPayloadClass, "defaultPayloadClass must not be null");
			this.defaultPayloadClass = defaultPayloadClass;
			return self();
		}

		@Override
		public O additionalHeaderForReceive(String name, Object value) {
			Assert.notNull(name, "name must not be null");
			Assert.notNull(value, "value must not be null");
			this.defaultAdditionalHeaders.put(name, value);
			return self();
		}

		@Override
		public O additionalHeadersForReceive(Map<String, Object> defaultAdditionalHeaders) {
			Assert.notNull(defaultAdditionalHeaders, "defaultAdditionalHeaders must not be null");
			this.defaultAdditionalHeaders.putAll(defaultAdditionalHeaders);
			return self();
		}

		@SuppressWarnings("unchecked")
		public O self() {
			return (O) this;
		}
	}

}
