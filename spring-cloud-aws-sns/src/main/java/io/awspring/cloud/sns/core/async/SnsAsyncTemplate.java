package io.awspring.cloud.sns.core.async;

import io.awspring.cloud.sns.core.CachingTopicArnResolver;
import io.awspring.cloud.sns.core.SnsAsyncTopicArnResolver;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.TopicArnResolver;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;

/**
 * Asynchronous template for SNS operations.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public class SnsAsyncTemplate implements SnsAsyncOperations {

	private final SnsAsyncClient snsAsyncClient;
	private final TopicArnResolver topicArnResolver;
	private final SnsPublishMessageConverter snsPublishMessageConverter;

	public SnsAsyncTemplate(SnsAsyncClient snsAsyncClient) {
		this(snsAsyncClient, null);
	}

	public SnsAsyncTemplate(SnsAsyncClient snsAsyncClient, @Nullable SnsPublishMessageConverter messageConverter) {
		this(snsAsyncClient, new CachingTopicArnResolver(new SnsAsyncTopicArnResolver(snsAsyncClient)), messageConverter);
	}

	public SnsAsyncTemplate(SnsAsyncClient snsAsyncClient, TopicArnResolver topicArnResolver,
							@Nullable SnsPublishMessageConverter messageConverter) {
		Assert.notNull(snsAsyncClient, "SnsAsyncClient must not be null");
		Assert.notNull(topicArnResolver, "TopicArnResolver must not be null");
		this.topicArnResolver = topicArnResolver;
		this.snsAsyncClient = snsAsyncClient;
		this.snsPublishMessageConverter = messageConverter == null ? new DefaultSnsPublishMessageConverter() : messageConverter;
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> send(String destination, Message<T> message) {
		Assert.notNull(destination, "destination cannot be null");
		Assert.notNull(message, "message cannot be null");

		Arn topicArn = topicArnResolver.resolveTopicArn(destination);

		PublishRequestMessagePair<T> pair = snsPublishMessageConverter.convert(message);
		PublishRequest request = pair.publishRequest()
			.toBuilder()
			.topicArn(topicArn.toString())
			.build();

		return publish(request, pair.originalMessage());
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload) {
		Assert.notNull(destination, "destination cannot be null");
		Assert.notNull(payload, "payload cannot be null");

		return convertAndSend(destination, payload, null, null);
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers) {
		Assert.notNull(destination, "destination cannot be null");
		Assert.notNull(payload, "payload cannot be null");

		return convertAndSend(destination, payload, headers, null);
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable MessagePostProcessor postProcessor) {
		Assert.notNull(destination, "destination cannot be null");
		Assert.notNull(payload, "payload cannot be null");

		return convertAndSend(destination, payload, null, postProcessor);
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers, @Nullable MessagePostProcessor postProcessor) {
		Assert.notNull(destination, "destination cannot be null");
		Assert.notNull(payload, "payload cannot be null");

		Message<T> message = MessageBuilder
			.withPayload(payload)
			.copyHeaders(headers != null ? headers : Collections.emptyMap())
			.build();

		if (postProcessor != null) {
			message = (Message<T>) postProcessor.postProcessMessage(message);
		}

		return send(destination, message);
	}

	@Override
	public CompletableFuture<SnsResult<Object>> sendNotification(String destinationName, Object message, @Nullable String subject) {
		Assert.notNull(destinationName, "destinationName cannot be null");
		Assert.notNull(message, "message cannot be null");

		return convertAndSend(destinationName, message, Collections.singletonMap(NOTIFICATION_SUBJECT_HEADER, subject));
	}

	@Override
	public <T> CompletableFuture<SnsResult<T>> sendNotification(String topic, SnsNotification<T> notification) {
		Assert.notNull(topic, "topic cannot be null");
		Assert.notNull(notification, "notification cannot be null");

		return convertAndSend(topic, notification.getPayload(), notification.getHeaders());
	}

	@Override
	public CompletableFuture<Boolean> topicExists(String topicArn) {
		Assert.notNull(topicArn, "topicArn must not be null");

		return snsAsyncClient.getTopicAttributes(request -> request.topicArn(topicArn))
			.thenApply(response -> true)
			.exceptionally(throwable -> {
				if (throwable.getCause() instanceof NotFoundException) {
					return false;
				}
				if (throwable.getCause() instanceof RuntimeException re) {
					throw re;
				}
				throw new RuntimeException("Unexpected exception", throwable);
			});
	}

	private <T> CompletableFuture<SnsResult<T>> publish(PublishRequest request, Message<T> originalMessage) {
		return snsAsyncClient.publish(request)
			.thenApply(response -> new SnsResult<>(
				originalMessage,
				response.messageId(),
				response.sequenceNumber()
			));
	}
}
