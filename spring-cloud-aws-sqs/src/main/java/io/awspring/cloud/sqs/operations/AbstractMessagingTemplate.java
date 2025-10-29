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

import io.awspring.cloud.sqs.ExceptionUtils;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.AsyncAdapterBlockingExecutionFailedException;
import io.awspring.cloud.sqs.listener.ListenerExecutionFailedException;
import io.awspring.cloud.sqs.support.converter.ContextAwareMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.observation.AbstractTemplateObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.docs.ObservationDocumentation;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
 * @param <S> the source message type for conversion
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessagingTemplate<S> implements MessagingOperations, AsyncMessagingOperations {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingTemplate.class);

	private static final TemplateAcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = TemplateAcknowledgementMode.ACKNOWLEDGE;

	private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

	private static final SendBatchFailureHandlingStrategy DEFAULT_SEND_BATCH_OPERATION_FAILURE_STRATEGY = SendBatchFailureHandlingStrategy.THROW;

	private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY = ObservationRegistry.NOOP;

	private static final int DEFAULT_MAX_NUMBER_OF_MESSAGES = 10;

	private static final String DEFAULT_ENDPOINT_NAME = "";

	private static final int MAX_ONE_MESSAGE = 1;

	private final Map<String, Object> defaultAdditionalHeaders;

	private final Duration defaultPollTimeout;

	private final int defaultMaxNumberOfMessages;

	private final String defaultEndpointName;

	private final TemplateAcknowledgementMode acknowledgementMode;

	private final SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy;

	private final AbstractTemplateObservation.Specifics<?> observationSpecifics;

	private final ObservationRegistry observationRegistry;

	@Nullable
	private final ObservationConvention<?> customObservationConvention;

	@Nullable
	private final Class<?> defaultPayloadClass;

	private final MessagingMessageConverter<S> messageConverter;

	protected AbstractMessagingTemplate(MessagingMessageConverter<S> messageConverter,
			AbstractMessagingTemplateOptions<?> options,
			AbstractTemplateObservation.Specifics<?> observationSpecifics) {
		Assert.notNull(observationSpecifics, "observationSpecifics must not be null");
		Assert.notNull(messageConverter, "messageConverter must not be null");
		Assert.notNull(options, "options must not be null");
		this.messageConverter = messageConverter;
		this.observationSpecifics = observationSpecifics;
		this.defaultAdditionalHeaders = options.defaultAdditionalHeaders;
		this.defaultMaxNumberOfMessages = options.defaultMaxNumberOfMessages;
		this.defaultPollTimeout = options.defaultPollTimeout;
		this.defaultPayloadClass = options.defaultPayloadClass;
		this.defaultEndpointName = options.defaultEndpointName;
		this.acknowledgementMode = options.acknowledgementMode;
		this.sendBatchFailureHandlingStrategy = options.sendBatchFailureHandlingStrategy;
		this.observationRegistry = options.observationRegistry;
		this.customObservationConvention = options.observationConvention;
	}

	@Override
	public Optional<Message<?>> receive() {
		return unwrapCompletionException(receiveAsync());
	}

	@Override
	public <T> Optional<Message<T>> receive(String queue, Class<T> payloadClass) {
		return unwrapCompletionException(receiveAsync(queue, payloadClass));
	}

	@Override
	public Collection<Message<?>> receiveMany() {
		return unwrapCompletionException(receiveManyAsync());
	}

	@Override
	public <T> Collection<Message<T>> receiveMany(String queue, Class<T> payloadClass) {
		Assert.notNull(queue, "queue cannot be null");
		Assert.notNull(payloadClass, "payloadClass cannot be null");
		return unwrapCompletionException(receiveManyAsync(queue, payloadClass));
	}

	@Override
	public CompletableFuture<Optional<Message<?>>> receiveAsync() {
		return receiveAsync(null, null, null, null);
	}

	@Override
	public <T> CompletableFuture<Optional<Message<T>>> receiveAsync(String queue, Class<T> payloadClass) {
		Assert.notNull(queue, "queue cannot be null");
		Assert.notNull(payloadClass, "payloadClass cannot be null");
		return receiveAsync(queue, payloadClass, null, null).thenApply(this::castFromOptional);
	}

	@SuppressWarnings("unchecked")
	protected <T> Optional<Message<T>> castFromOptional(Optional<Message<?>> optional) {
		return optional.map(msg -> (Message<T>) msg);
	}

	@SuppressWarnings("unchecked")
	protected <T> List<Message<T>> castFromCollection(Collection<Message<?>> msgs) {
		return msgs.stream().map(msg -> (Message<T>) msg).toList();
	}

	protected CompletableFuture<Optional<Message<?>>> receiveAsync(@Nullable String endpoint,
			@Nullable Class<?> payloadClass, @Nullable Duration pollTimeout,
			@Nullable Map<String, Object> additionalHeaders) {
		return receiveManyAsync(endpoint, payloadClass, pollTimeout, MAX_ONE_MESSAGE, additionalHeaders)
				.thenApply(msgs -> msgs.isEmpty() ? Optional.empty() : Optional.of(msgs.iterator().next()));
	}

	@Override
	public CompletableFuture<Collection<Message<?>>> receiveManyAsync() {
		return receiveManyAsync(null, null, null, null, null);
	}

	@Override
	public <T> CompletableFuture<Collection<Message<T>>> receiveManyAsync(String queue, Class<T> payloadClass) {
		return receiveManyAsync(queue, payloadClass, null, null, null).thenApply(this::castFromCollection);
	}

	protected CompletableFuture<Collection<Message<?>>> receiveManyAsync(@Nullable String endpoint,
			@Nullable Class<?> payloadClass, @Nullable Duration pollTimeout, @Nullable Integer maxNumberOfMessages,
			@Nullable Map<String, Object> additionalHeaders) {
		String endpointToUse = getEndpointName(endpoint);
		logger.trace("Receiving messages from endpoint {}", endpointToUse);
		Map<String, Object> headers = getAdditionalHeadersToReceive(endpointToUse, additionalHeaders);
		Duration pollTimeoutToUse = getOrDefault(pollTimeout, this.defaultPollTimeout, "pollTimeout");
		Integer maxNumberOfMessagesToUse = getOrDefault(maxNumberOfMessages, this.defaultMaxNumberOfMessages,
				"defaultMaxNumberOfMessages");
		return doReceiveAsync(endpointToUse, pollTimeoutToUse, maxNumberOfMessagesToUse, headers)
				.thenApply(messages -> convertReceivedMessages(endpointToUse, payloadClass, messages, headers))
				.thenCompose(messages -> handleAcknowledgement(endpointToUse, messages))
				.exceptionallyCompose(t -> CompletableFuture.failedFuture(new MessagingOperationFailedException(
						"Message receive operation failed for endpoint %s".formatted(endpointToUse), endpointToUse,
						t instanceof CompletionException ? t.getCause() : t)))
				.whenComplete((v, t) -> logReceiveMessageResult(endpointToUse, v, t));
	}

	protected abstract Map<String, Object> preProcessHeadersForReceive(String endpointToUse,
			Map<String, Object> additionalHeaders);

	private Map<String, Object> getAdditionalHeadersToReceive(String endpointToUse,
			@Nullable Map<String, Object> additionalHeaders) {
		Map<String, Object> headers = new HashMap<>(this.defaultAdditionalHeaders);
		if (additionalHeaders != null) {
			headers.putAll(additionalHeaders);
		}
		return preProcessHeadersForReceive(endpointToUse, headers);
	}

	private Collection<Message<?>> convertReceivedMessages(String endpoint, @Nullable Class<?> payloadClass,
			Collection<S> messages, Map<String, Object> additionalHeaders) {
		return messages.stream()
				.map(message -> convertReceivedMessage(getEndpointName(endpoint), message,
						payloadClass != null ? payloadClass : this.defaultPayloadClass))
				.map(message -> addAdditionalHeaders(message, additionalHeaders)).collect(Collectors.toList());
	}

	protected <T> Message<T> addAdditionalHeaders(Message<T> message, Map<String, Object> additionalHeaders) {
		Map<String, Object> headersToAdd = handleAdditionalHeaders(additionalHeaders);
		return headersToAdd.isEmpty() ? message : MessageHeaderUtils.addHeadersIfAbsent(message, headersToAdd);
	}

	protected abstract Map<String, Object> handleAdditionalHeaders(Map<String, Object> additionalHeaders);

	private CompletableFuture<Collection<Message<?>>> handleAcknowledgement(@Nullable String endpoint,
			Collection<Message<?>> messages) {
		return TemplateAcknowledgementMode.ACKNOWLEDGE.equals(this.acknowledgementMode) && !messages.isEmpty()
				? doAcknowledgeMessages(getEndpointName(endpoint), messages).thenApply(theVoid -> messages)
				: CompletableFuture.completedFuture(messages);
	}

	protected abstract CompletableFuture<Void> doAcknowledgeMessages(String endpointName,
			Collection<Message<?>> messages);

	private String getEndpointName(@Nullable String endpoint) {
		String endpointName = getOrDefault(endpoint, this.defaultEndpointName, "endpointName");
		Assert.hasText(endpointName, "No endpoint name informed and no default value available");
		return endpointName;
	}

	private <V> V getOrDefault(@Nullable V value, V defaultValue, String valueName) {
		return Objects.requireNonNull(value != null ? value : defaultValue,
				valueName + " not set and no default value provided");
	}

	private Message<?> convertReceivedMessage(String endpoint, S message, @Nullable Class<?> payloadClass) {
		return this.messageConverter instanceof ContextAwareMessagingMessageConverter<S> contextConverter
				? contextConverter.toMessagingMessage(message,
						getReceiveMessageConversionContext(endpoint, payloadClass))
				: this.messageConverter.toMessagingMessage(message);
	}

	private void logReceiveMessageResult(String endpoint, @Nullable Collection<Message<?>> messages,
			@Nullable Throwable t) {
		if (messages != null) {
			logger.trace("Received messages {} from endpoint {}", MessageHeaderUtils.getId(addTypeToMessages(messages)),
					endpoint);
		}
		else {
			logger.error("Error receiving messages", t);
		}
	}

	@SuppressWarnings("unchecked")
	protected Collection<Message<Object>> addTypeToMessages(Collection<Message<?>> messages) {
		return messages.stream().map(message -> (Message<Object>) message).collect(Collectors.toList());
	}

	protected abstract CompletableFuture<Collection<S>> doReceiveAsync(String endpointName, Duration pollTimeout,
			Integer maxNumberOfMessages, Map<String, Object> additionalHeaders);

	@Override
	public <T> SendResult<T> send(T payload) {
		return unwrapCompletionException(sendAsync(payload));
	}

	@Override
	public <T> SendResult<T> send(@Nullable String endpointName, T payload) {
		return unwrapCompletionException(sendAsync(endpointName, payload));
	}

	@Override
	public <T> SendResult<T> send(@Nullable String endpointName, Message<T> message) {
		return unwrapCompletionException(sendAsync(endpointName, message));
	}

	@Override
	public <T> SendResult.Batch<T> sendMany(@Nullable String endpointName, Collection<Message<T>> messages) {
		return unwrapCompletionException(sendManyAsync(endpointName, messages));
	}

	@Override
	public <T> CompletableFuture<SendResult<T>> sendAsync(T payload) {
		return sendAsync(null, payload);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> CompletableFuture<SendResult<T>> sendAsync(@Nullable String endpointName, T payload) {
		return sendAsync(endpointName,
				payload instanceof Message ? (Message<T>) payload : MessageBuilder.withPayload(payload).build());
	}

	@Override
	public <T> CompletableFuture<SendResult<T>> sendAsync(@Nullable String endpointName, Message<T> message) {
		String endpointToUse = getEndpointName(endpointName);
		logger.trace("Sending message {} to endpoint {}", MessageHeaderUtils.getId(message), endpointName);
		return preProcessMessageForSendAsync(endpointToUse, message)
				.thenCompose(messageToUse -> observeAndSendAsync(messageToUse, endpointToUse)
						.exceptionallyCompose(
								t -> CompletableFuture.failedFuture(new MessagingOperationFailedException(
										"Message send operation failed for message %s to endpoint %s"
												.formatted(MessageHeaderUtils.getId(message), endpointToUse),
										endpointToUse, message, t)))
						.whenComplete((v, t) -> logSendMessageResult(endpointToUse, message, t)));
	}

	private <T> CompletableFuture<SendResult<T>> observeAndSendAsync(Message<T> message, String endpointToUse) {
		AbstractTemplateObservation.Context context = this.observationSpecifics.createContext(message, endpointToUse);
		Observation observation = startObservation(context);
		Map<String, Object> carrier = Objects.requireNonNull(context.getCarrier(), "No carrier found in context.");
		Message<T> messageWithObservationHeader = MessageHeaderUtils.addHeadersIfAbsent(message, carrier);
		return doSendAsync(endpointToUse, convertMessageToSend(messageWithObservationHeader),
				messageWithObservationHeader)
				.whenComplete((sendResult, t) -> completeObservation(sendResult, context, t, observation));
	}

	private void completeObservation(@Nullable SendResult<?> sendResult, AbstractTemplateObservation.Context context,
			@Nullable Throwable t, Observation observation) {
		if (sendResult != null) {
			context.setSendResult(sendResult);
		}
		if (t != null) {
			observation.error(ExceptionUtils.unwrapException(t, CompletionException.class,
					AsyncAdapterBlockingExecutionFailedException.class, ListenerExecutionFailedException.class));
		}

		observation.stop();
	}

	@SuppressWarnings("unchecked")
	private <Context extends Observation.Context> Observation startObservation(Context observationContext) {
		ObservationConvention<Context> defaultConvention = (ObservationConvention<Context>) observationSpecifics
				.getDefaultConvention();
		ObservationConvention<Context> customConvention = (ObservationConvention<Context>) this.customObservationConvention;
		ObservationDocumentation documentation = observationSpecifics.getDocumentation();
		return documentation.start(customConvention, defaultConvention, () -> observationContext,
				this.observationRegistry);
	}

	protected abstract <T> Message<T> preProcessMessageForSend(String endpointToUse, Message<T> message);

	protected <T> CompletableFuture<Message<T>> preProcessMessageForSendAsync(String endpointToUse,
			Message<T> message) {
		return CompletableFuture.completedFuture(preProcessMessageForSend(endpointToUse, message));
	}

	@Override
	public <T> CompletableFuture<SendResult.Batch<T>> sendManyAsync(@Nullable String endpointName,
			Collection<Message<T>> messages) {
		logger.trace("Sending messages {} to endpoint {}", MessageHeaderUtils.getId(messages), endpointName);
		String endpointToUse = getEndpointName(endpointName);
		return preProcessMessagesForSendAsync(endpointToUse, messages).thenCompose(
				messagesToUse -> doSendBatchAsync(endpointToUse, convertMessagesToSend(messagesToUse), messagesToUse)
						.exceptionallyCompose(t -> wrapSendException(messagesToUse, endpointToUse, t))
						.thenCompose(result -> handleFailedMessages(endpointToUse, result))
						.whenComplete((v, t) -> logSendMessageBatchResult(endpointToUse, messagesToUse, t)));
	}

	protected abstract <T> Collection<Message<T>> preProcessMessagesForSend(String endpointToUse,
			Collection<Message<T>> messages);

	protected <T> CompletableFuture<Collection<Message<T>>> preProcessMessagesForSendAsync(String endpointToUse,
			Collection<Message<T>> messages) {
		return CompletableFuture.completedFuture(preProcessMessagesForSend(endpointToUse, messages));
	}

	private <T> CompletableFuture<SendResult.Batch<T>> handleFailedMessages(String endpointToUse,
			SendResult.Batch<T> result) {
		return !result.failed().isEmpty()
				&& SendBatchFailureHandlingStrategy.THROW.equals(this.sendBatchFailureHandlingStrategy)
						? handleFailedSendBatch(endpointToUse, result)
						: CompletableFuture.completedFuture(result);
	}

	private <T> CompletableFuture<SendResult.Batch<T>> wrapSendException(Collection<Message<T>> messages,
			String endpointToUse, Throwable t) {
		return CompletableFuture.failedFuture(
				new MessagingOperationFailedException("Message send operation failed for messages %s to endpoint %s"
						.formatted(MessageHeaderUtils.getId(messages), endpointToUse), endpointToUse, messages, t));
	}

	private <T> CompletableFuture<SendResult.Batch<T>> handleFailedSendBatch(String endpoint,
			SendResult.Batch<T> result) {
		return CompletableFuture.failedFuture(new SendBatchOperationFailedException("", endpoint, result));
	}

	private <T> Collection<S> convertMessagesToSend(Collection<Message<T>> messages) {
		return messages.stream().map(this::convertMessageToSend).collect(Collectors.toList());
	}

	private <T> S convertMessageToSend(Message<T> message) {
		return this.messageConverter instanceof ContextAwareMessagingMessageConverter<S> contextConverter
				? contextConverter.fromMessagingMessage(message, getSendMessageConversionContext(message))
				: messageConverter.fromMessagingMessage(message);
	}

	protected abstract <T> CompletableFuture<SendResult<T>> doSendAsync(String endpointName, S message,
			Message<T> originalMessage);

	protected abstract <T> CompletableFuture<SendResult.Batch<T>> doSendBatchAsync(String endpointName,
			Collection<S> messages, Collection<Message<T>> originalMessages);

	@Nullable
	protected <T> MessageConversionContext getReceiveMessageConversionContext(String endpointName,
			@Nullable Class<T> payloadClass) {
		// Subclasses can override this method to return a context
		return null;
	}

	@Nullable
	protected <T> MessageConversionContext getSendMessageConversionContext(Message<T> message) {
		// Subclasses can override this method to return a context
		return null;
	}

	private <T> void logSendMessageResult(String endpointToUse, Message<T> message, @Nullable Throwable t) {
		if (t == null) {
			logger.trace("Message {} successfully sent to endpoint {} with id {}", message, endpointToUse,
					MessageHeaderUtils.getId(message));
		}
		else {
			logger.error("Error sending message {} to endpoint {}", MessageHeaderUtils.getId(message), endpointToUse,
					unwrapCompletionException(t));
		}
	}

	private Throwable unwrapCompletionException(Throwable t) {
		return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
	}

	private <T> void logSendMessageBatchResult(String endpointToUse, Collection<Message<T>> messages,
			@Nullable Throwable t) {
		if (t == null) {
			logger.trace("Messages {} successfully sent to endpoint {} with id {}", messages, endpointToUse,
					MessageHeaderUtils.getId(messages));
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
			if (ex.getCause() instanceof RuntimeException re) {
				throw re;
			}
			throw new RuntimeException("Unexpected exception", ex);
		}
	}

	/**
	 * Base class for template options, to be extended by subclasses.
	 * @param <O> the options type for returning in the chained methods.
	 */
	protected static abstract class AbstractMessagingTemplateOptions<O extends MessagingTemplateOptions<O>>
			implements MessagingTemplateOptions<O> {

		private Duration defaultPollTimeout = DEFAULT_POLL_TIMEOUT;

		private int defaultMaxNumberOfMessages = DEFAULT_MAX_NUMBER_OF_MESSAGES;

		private String defaultEndpointName = DEFAULT_ENDPOINT_NAME;

		private TemplateAcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

		private SendBatchFailureHandlingStrategy sendBatchFailureHandlingStrategy = DEFAULT_SEND_BATCH_OPERATION_FAILURE_STRATEGY;

		private final Map<String, Object> defaultAdditionalHeaders = new HashMap<>();

		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

		@Nullable
		private ObservationConvention<?> observationConvention;

		@Nullable
		private Class<?> defaultPayloadClass;

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

		/**
		 * Subclasses should have a method with a more specific name and delegate to this.
		 * @param defaultEndpointName the default endpoint name.
		 */
		protected void defaultEndpointName(String defaultEndpointName) {
			Assert.notNull(defaultEndpointName, "defaultEndpointName must not be null");
			this.defaultEndpointName = defaultEndpointName;
		}

		@Override
		public O defaultPayloadClass(Class<?> defaultPayloadClass) {
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

		@Override
		public O observationRegistry(ObservationRegistry observationRegistry) {
			Assert.notNull(observationRegistry, "observationRegistry cannot be null");
			this.observationRegistry = observationRegistry;
			return self();
		}

		protected O observationConvention(ObservationConvention<?> observationConvention) {
			Assert.notNull(observationConvention, "observationConvention cannot be null");
			this.observationConvention = observationConvention;
			return self();
		}

		@SuppressWarnings("unchecked")
		public O self() {
			return (O) this;
		}
	}

}
