package io.awspring.cloud.sqs.operations;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.ContextAwareMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class AbstractMessagingTemplate<T, S, R> implements MessagingOperations<T, R>,
	AsyncMessagingOperations<T, R> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessagingTemplate.class);

	private static final TemplateAcknowledgementMode DEFAULT_ACKNOWLEDGEMENT_MODE = TemplateAcknowledgementMode.ACKNOWLEDGE_ON_RECEIVE;

	private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofSeconds(10);

	private static final int DEFAULT_MAX_NUMBER_OF_MESSAGES = 10;

	private static final String DEFAULT_ENDPOINT_NAME = "";

	private final Map<String, Object> defaultAdditionalHeaders;

	private final Duration defaultPollTimeout;

	private final int defaultMaxNumberOfMessages;

	private final String defaultEndpointName;

	private final TemplateAcknowledgementMode acknowledgementMode;

	@Nullable
	private final Class<T> defaultPayloadClass;

	private final MessagingMessageConverter<S> messageConverter;

	protected AbstractMessagingTemplate(MessagingMessageConverter<S> messageConverter, AbstractMessagingTemplateOptions<T, ?> options) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		Assert.notNull(options, "options must not be null");
		this.messageConverter = messageConverter;
		this.defaultAdditionalHeaders = options.defaultAdditionalHeaders;
		this.defaultMaxNumberOfMessages = options.defaultMaxNumberOfMessages;
		this.defaultPollTimeout = options.defaultPollTimeout;
		this.defaultPayloadClass = options.defaultPayloadClass;
		this.defaultEndpointName = options.defaultEndpointName;
		this.acknowledgementMode = options.acknowledgementMode;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		Assert.isInstanceOf(AbstractMessagingMessageConverter.class, this.messageConverter,
			"objectMappers can only be set on AbstractMessagingMessageConverter instances.");
		((AbstractMessagingMessageConverter<?>) this.messageConverter).setObjectMapper(objectMapper);
	}

	@Override
	public Optional<Message<T>> receive() {
		return receiveAsync().join();
	}

	@Override
	public Optional<Message<T>> receive(@Nullable String endpoint, @Nullable Class<T> payloadClass, @Nullable Duration pollTimeout, @Nullable Map<String, Object> additionalHeaders) {
		return receiveAsync(endpoint, payloadClass, pollTimeout, additionalHeaders).join();
	}

	@Override
	public Collection<Message<T>> receiveMany() {
		return receiveManyAsync().join();
	}

	@Override
	public Collection<Message<T>> receiveMany(@Nullable String endpoint, @Nullable Class<T> payloadClass,
											  @Nullable Duration pollTimeout, @Nullable Integer maxNumberOfMessages,
											  @Nullable Map<String, Object> additionalHeaders) {
		return receiveManyAsync(endpoint, payloadClass, pollTimeout, maxNumberOfMessages, additionalHeaders).join();
	}

	@Override
	public CompletableFuture<Optional<Message<T>>> receiveAsync() {
		return receiveAsync(null, null, null, null);
	}

	@Override
	public CompletableFuture<Optional<Message<T>>> receiveAsync(@Nullable String endpoint, @Nullable Class<T> payloadClass,
																@Nullable Duration pollTimeout, @Nullable Map<String, Object> additionalHeaders) {
		return receiveManyAsync(endpoint, payloadClass, pollTimeout, 1, additionalHeaders)
			.thenApply(msgs -> msgs.isEmpty() ? Optional.empty() : Optional.of(msgs.iterator().next()));
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> receiveManyAsync() {
		return receiveManyAsync(this.defaultEndpointName, this.defaultPayloadClass, this.defaultPollTimeout,
			this.defaultMaxNumberOfMessages, this.defaultAdditionalHeaders);
	}

	@Override
	public CompletableFuture<Collection<Message<T>>> receiveManyAsync(@Nullable String endpoint, @Nullable Class<T> payloadClass, @Nullable Duration pollTimeout,
																	  @Nullable Integer maxNumberOfMessages, @Nullable Map<String, Object> additionalHeaders) {
		Map<String, Object> headers = getAdditionalHeadersToReceive(additionalHeaders);
		return doReceiveAsync(getEndpointName(endpoint),
			getOrDefault(pollTimeout, this.defaultPollTimeout, "pollTimeout"),
			getOrDefault(maxNumberOfMessages, this.defaultMaxNumberOfMessages, "defaultMaxNumberOfMessages"),
			headers)
			.thenApply(messages -> convertReceivedMessages(getEndpointName(endpoint), payloadClass, messages, headers))
			.thenCompose(messages -> handleAcknowledgement(getEndpointName(endpoint), messages));
	}

	private Map<String, Object> getAdditionalHeadersToReceive(@Nullable Map<String, Object> additionalHeaders) {
		Map<String, Object> headers = new HashMap<>(this.defaultAdditionalHeaders);
		if (additionalHeaders != null) {
			headers.putAll(additionalHeaders);
		}
		return headers;
	}

	private Collection<Message<T>> convertReceivedMessages(String endpoint, @Nullable Class<T> payloadClass,
														   Collection<S> messages, Map<String, Object> additionalHeaders) {
		return messages.stream().map(message -> convertReceivedMessage(getEndpointName(endpoint), message,
				payloadClass != null ? payloadClass : this.defaultPayloadClass))
			.map(message -> addAdditionalHeaders(message, additionalHeaders))
			.collect(Collectors.toList());
	}

	protected Message<T> addAdditionalHeaders(Message<T> message, Map<String, Object> additionalHeaders) {
		Map<String, Object> headersToAdd = handleAdditionalHeaders(additionalHeaders);
		return headersToAdd.isEmpty()
			? message
			: MessageBuilder.createMessage(message.getPayload(), doAddAdditionalHeaders(message.getHeaders(), headersToAdd));
	}

	private MessageHeaders doAddAdditionalHeaders(Map<String, Object> headers, Map<String, Object> headersToAdd) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeaders(headers);
		accessor.copyHeaders(headersToAdd);
		return accessor.toMessageHeaders();
	}

	protected abstract Map<String, Object> handleAdditionalHeaders(Map<String, Object> additionalHeaders);

	private CompletableFuture<Collection<Message<T>>> handleAcknowledgement(@Nullable String endpoint, Collection<Message<T>> messages) {
		return TemplateAcknowledgementMode.ACKNOWLEDGE_ON_RECEIVE.equals(this.acknowledgementMode) && !messages.isEmpty()
			? acknowledgeMessages(getEndpointName(endpoint), messages).thenApply(theVoid -> messages)
			: CompletableFuture.completedFuture(messages);
	}

	protected abstract CompletableFuture<Void> acknowledgeMessages(String endpointName, Collection<Message<T>> messages);

	private String getEndpointName(@Nullable String endpoint) {
		return getOrDefault(endpoint, this.defaultEndpointName, "endpointName");
	}

	private <V> V getOrDefault(@Nullable V value, V defaultValue, String valueName) {
		return Objects.requireNonNull(value != null ? value : defaultValue, valueName + " not set and no default value provided");
	}

	@SuppressWarnings("unchecked")
	private Message<T> convertReceivedMessage(String endpoint, S message, @Nullable Class<T> payloadClass) {
		return this.messageConverter instanceof ContextAwareMessagingMessageConverter<S> contextConverter
			? (Message<T>) contextConverter.toMessagingMessage(message, getMessageConversionContext(endpoint, payloadClass))
			: (Message<T>) this.messageConverter.toMessagingMessage(message);
	}

	protected abstract CompletableFuture<Collection<S>> doReceiveAsync(String endpointName, Duration pollTimeout, Integer maxNumberOfMessages, Map<String, Object> additionalHEaders);

	@Override
	public UUID send(T payload) {
		return sendAsync(payload).join();
	}

	@Override
	public UUID send(@Nullable String endpointName, T payload) {
		return sendAsync(endpointName, payload).join();
	}

	@Override
	public UUID send(@Nullable String endpointName, Message<T> message) {
		return sendAsync(endpointName, message).join();
	}

	@Override
	public R send(@Nullable String endpointName, Collection<Message<T>> messages) {
		return sendAsync(endpointName, messages).join();
	}

	@Override
	public CompletableFuture<UUID> sendAsync(T payload) {
		return sendAsync(null, payload);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CompletableFuture<UUID> sendAsync(@Nullable String endpointName, T payload) {
		return sendAsync(endpointName, payload instanceof Message ? (Message<T>) payload : MessageBuilder.withPayload(payload).build());
	}

	@Override
	public CompletableFuture<UUID> sendAsync(@Nullable String endpointName, Message<T> message) {
		return doSendAsync(getOrDefault(endpointName, this.defaultEndpointName, "endpointName"),
			convertMessageToSend(message));
	}

	@Override
	public CompletableFuture<R> sendAsync(@Nullable String endpointName, Collection<Message<T>> messages) {
		return doSendBatchAsync(getOrDefault(endpointName, this.defaultEndpointName, "endpointName"),
			convertMessagesToSend(messages));
	}

	private Collection<S> convertMessagesToSend(Collection<Message<T>> messages) {
		return messages.stream().map(this::convertMessageToSend).collect(Collectors.toList());
	}

	private S convertMessageToSend(Message<T> message) {
		return this.messageConverter.fromMessagingMessage(message);
	}

	protected abstract CompletableFuture<UUID> doSendAsync(String endpointName, S message);

	protected abstract CompletableFuture<R> doSendBatchAsync(String endpointName, Collection<S> messages);

	@Nullable
	protected MessageConversionContext getMessageConversionContext(String endpointName, @Nullable Class<T> payloadClass) {
		// Subclasses can override this to return a context
		return null;
	}

	protected interface MessagingTemplateOptions<T, O extends MessagingTemplateOptions<T, O>> {

		O acknowledgementMode(TemplateAcknowledgementMode defaultAcknowledgementMode);

		O pollTimeout(Duration defaultPollTimeout);

		O maxNumberOfMessages(Integer defaultMaxNumberOfMessages);

		O endpointName(String defaultEndpointName);

		O payloadClass(Class<T> defaultPayloadClass);

		O additionalHeader(String name, Object value);

		O additionalHeaders(Map<String, Object> defaultAdditionalHeaders);

	}

	protected static abstract class AbstractMessagingTemplateOptions<T, O extends MessagingTemplateOptions<T, O>> implements MessagingTemplateOptions<T, O> {

		private Duration defaultPollTimeout = DEFAULT_POLL_TIMEOUT;

		private int defaultMaxNumberOfMessages = DEFAULT_MAX_NUMBER_OF_MESSAGES;

		private String defaultEndpointName = DEFAULT_ENDPOINT_NAME;

		private TemplateAcknowledgementMode acknowledgementMode = DEFAULT_ACKNOWLEDGEMENT_MODE;

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
		public O pollTimeout(Duration defaultPollTimeout) {
			Assert.notNull(defaultPollTimeout, "pollTimeout must not be null");
			this.defaultPollTimeout = defaultPollTimeout;
			return self();
		}

		@Override
		public O maxNumberOfMessages(Integer defaultMaxNumberOfMessages) {
			Assert.isTrue(defaultMaxNumberOfMessages > 0, "defaultMaxNumberOfMessages must be greater than zero");
			this.defaultMaxNumberOfMessages = defaultMaxNumberOfMessages;
			return self();
		}

		@Override
		public O endpointName(String defaultEndpointName) {
			Assert.hasText(defaultEndpointName, "defaultEndpointName must have text");
			this.defaultEndpointName = defaultEndpointName;
			return self();
		}

		@Override
		public O payloadClass(Class<T> defaultPayloadClass) {
			Assert.notNull(defaultPayloadClass, "defaultPayloadClass must not be null");
			this.defaultPayloadClass = defaultPayloadClass;
			return self();
		}

		@Override
		public O additionalHeader(String name, Object value) {
			Assert.notNull(name, "name must not be null");
			Assert.notNull(value, "value must not be null");
			this.defaultAdditionalHeaders.put(name, value);
			return self();
		}

		@Override
		public O additionalHeaders(Map<String, Object> defaultAdditionalHeaders) {
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
