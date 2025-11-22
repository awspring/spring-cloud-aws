/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

import com.amazonaws.services.schemaregistry.common.Schema;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.context.Lifecycle;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.kinesis.producer.KinesisProducer;
import software.amazon.kinesis.producer.UserRecord;
import software.amazon.kinesis.producer.UserRecordResult;

/**
 * The {@link AbstractMessageHandler} implementation for the Amazon Kinesis Producer Library {@code putRecord(s)}.
 * <p>
 * The {@link KplBackpressureException} is thrown when backpressure handling is enabled and buffer is at max capacity.
 * This exception can be handled with
 * {@link org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice}.
 * </p>
 *
 * @author Arnaud Lecollaire
 * @author Artem Bilan
 * @author Siddharth Jain
 *
 * @since 4.0
 */
public class KplMessageHandler extends AbstractMessageProducingHandler implements Lifecycle {

	private static final long DEFAULT_SEND_TIMEOUT = 10000;

	private final KinesisProducer kinesisProducer;

	private MessageConverter messageConverter = new ConvertingFromMessageConverter(new SerializingConverter());

	private Expression streamExpression;

	private Expression partitionKeyExpression;

	private Expression explicitHashKeyExpression;

	private Expression glueSchemaExpression;

	private OutboundMessageMapper<byte[]> embeddedHeadersMapper;

	private Duration flushDuration = Duration.ofMillis(0);

	private Expression sendTimeoutExpression = new ValueExpression<>(DEFAULT_SEND_TIMEOUT);

	private EvaluationContext evaluationContext;

	private volatile boolean running;

	private volatile ScheduledFuture<?> flushFuture;

	private long backPressureThreshold = 0;

	public KplMessageHandler(KinesisProducer kinesisProducer) {
		Assert.notNull(kinesisProducer, "'kinesisProducer' must not be null.");
		this.kinesisProducer = kinesisProducer;
	}

	/**
	 * Configure maximum records in flight for handling backpressure. By default, backpressure handling is not enabled.
	 * When backpressure handling is enabled and number of records in flight exceeds the threshold, a
	 * {@link KplBackpressureException} would be thrown.
	 * @param backPressureThreshold a value greater than {@code 0} to enable backpressure handling.
	 */
	public void setBackPressureThreshold(long backPressureThreshold) {
		Assert.isTrue(backPressureThreshold >= 0, "'backPressureThreshold must be greater than or equal to 0.");
		this.backPressureThreshold = backPressureThreshold;
	}

	/**
	 * Configure a {@link MessageConverter} for converting payload to {@code byte[]} for Kinesis record.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null.");
		this.messageConverter = messageConverter;
	}

	public void setStream(String stream) {
		setStreamExpression(new LiteralExpression(stream));
	}

	public void setStreamExpressionString(String streamExpression) {
		setStreamExpression(EXPRESSION_PARSER.parseExpression(streamExpression));
	}

	public void setStreamExpression(Expression streamExpression) {
		this.streamExpression = streamExpression;
	}

	public void setPartitionKey(String partitionKey) {
		setPartitionKeyExpression(new LiteralExpression(partitionKey));
	}

	public void setPartitionKeyExpressionString(String partitionKeyExpression) {
		setPartitionKeyExpression(EXPRESSION_PARSER.parseExpression(partitionKeyExpression));
	}

	public void setPartitionKeyExpression(Expression partitionKeyExpression) {
		this.partitionKeyExpression = partitionKeyExpression;
	}

	public void setExplicitHashKey(String explicitHashKey) {
		setExplicitHashKeyExpression(new LiteralExpression(explicitHashKey));
	}

	public void setExplicitHashKeyExpressionString(String explicitHashKeyExpression) {
		setExplicitHashKeyExpression(EXPRESSION_PARSER.parseExpression(explicitHashKeyExpression));
	}

	public void setExplicitHashKeyExpression(Expression explicitHashKeyExpression) {
		this.explicitHashKeyExpression = explicitHashKeyExpression;
	}

	/**
	 * Specify a {@link OutboundMessageMapper} for embedding message headers into the record data together with payload.
	 * @param embeddedHeadersMapper the {@link OutboundMessageMapper} to embed headers into the record data.
	 */
	public void setEmbeddedHeadersMapper(OutboundMessageMapper<byte[]> embeddedHeadersMapper) {
		this.embeddedHeadersMapper = embeddedHeadersMapper;
	}

	/**
	 * Configure a {@link Duration} how often to call a {@link KinesisProducer#flush()}.
	 * @param flushDuration the {@link Duration} to periodic call of a {@link KinesisProducer#flush()}.
	 */
	public void setFlushDuration(Duration flushDuration) {
		Assert.notNull(flushDuration, "'flushDuration' must not be null.");
		this.flushDuration = flushDuration;
	}

	/**
	 * Set a {@link Schema} to add into a {@link UserRecord} built from the request message.
	 * @param glueSchema the {@link Schema} to add into a {@link UserRecord}.
	 * @see UserRecord#setSchema(Schema)
	 */
	public void setGlueSchema(Schema glueSchema) {
		setGlueSchemaExpression(new ValueExpression<>(glueSchema));
	}

	/**
	 * Set a SpEL expression for {@link Schema} to add into a {@link UserRecord} built from the request message.
	 * @param glueSchemaExpression the SpEL expression to evaluate a {@link Schema}.
	 * @see UserRecord#setSchema(Schema)
	 */
	public void setGlueSchemaExpressionString(String glueSchemaExpression) {
		setGlueSchemaExpression(EXPRESSION_PARSER.parseExpression(glueSchemaExpression));
	}

	/**
	 * Set a SpEL expression for {@link Schema} to add into a {@link UserRecord} built from the request message.
	 * @param glueSchemaExpression the SpEL expression to evaluate a {@link Schema}.
	 * @see UserRecord#setSchema(Schema)
	 */
	public void setGlueSchemaExpression(Expression glueSchemaExpression) {
		this.glueSchemaExpression = glueSchemaExpression;
	}

	public void setSendTimeout(long sendTimeout) {
		setSendTimeoutExpression(new ValueExpression<>(sendTimeout));
	}

	public void setSendTimeoutExpressionString(String sendTimeoutExpression) {
		setSendTimeoutExpression(EXPRESSION_PARSER.parseExpression(sendTimeoutExpression));
	}

	public void setSendTimeoutExpression(Expression sendTimeoutExpression) {
		Assert.notNull(sendTimeoutExpression, "'sendTimeoutExpression' must not be null");
		this.sendTimeoutExpression = sendTimeoutExpression;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	public synchronized void start() {
		if (!this.running) {
			if (this.flushDuration.toMillis() > 0) {
				this.flushFuture = getTaskScheduler().scheduleAtFixedRate(this.kinesisProducer::flush,
						this.flushDuration);
			}
			this.running = true;
		}
	}

	@Override
	public synchronized void stop() {
		if (this.running) {
			this.running = false;
			if (this.flushFuture != null) {
				this.flushFuture.cancel(true);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		UserRecord request = messageToUserRecord(message);

		CompletableFuture<?> resultFuture = handleMessageToAws(request)
				.handle((response, ex) -> handleResponse(message, response, ex));

		if (isAsync()) {
			sendOutputs(resultFuture, message);
			return;
		}

		Long sendTimeout = this.sendTimeoutExpression.getValue(this.evaluationContext, message, Long.class);

		try {
			if (sendTimeout == null || sendTimeout < 0) {
				resultFuture.get();
			}
			else {
				resultFuture.get(sendTimeout, TimeUnit.MILLISECONDS);
			}
		}
		catch (TimeoutException te) {
			throw new MessageTimeoutException(message, "Timeout waiting for response from KinesisProducer", te);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
		catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private UserRecord messageToUserRecord(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof UserRecord userRecord) {
			return userRecord;
		}

		return buildUserRecord(message);
	}

	private UserRecord buildUserRecord(Message<?> message) {
		Object payload = message.getPayload();

		MessageHeaders messageHeaders = message.getHeaders();
		String stream = messageHeaders.get(KinesisHeaders.STREAM, String.class);
		if (!StringUtils.hasText(stream) && this.streamExpression != null) {
			stream = this.streamExpression.getValue(this.evaluationContext, message, String.class);
		}
		Assert.state(stream != null,
				"'stream' must not be null for sending a Kinesis record. "
						+ "Consider configuring this handler with a 'stream'( or 'streamExpression') or supply an "
						+ "'aws_stream' message header.");

		String partitionKey = messageHeaders.get(KinesisHeaders.PARTITION_KEY, String.class);
		if (!StringUtils.hasText(partitionKey) && this.partitionKeyExpression != null) {
			partitionKey = this.partitionKeyExpression.getValue(this.evaluationContext, message, String.class);
		}
		Assert.state(partitionKey != null,
				"'partitionKey' must not be null for sending a Kinesis record."
						+ "Consider configuring this handler with a 'partitionKey'( or 'partitionKeyExpression') "
						+ "or supply an 'aws_partitionKey' message header.");

		String explicitHashKey = this.explicitHashKeyExpression != null
				? this.explicitHashKeyExpression.getValue(this.evaluationContext, message, String.class)
				: null;

		Schema schema = this.glueSchemaExpression != null
				? this.glueSchemaExpression.getValue(this.evaluationContext, message, Schema.class)
				: null;

		Message<?> messageToEmbed = null;
		ByteBuffer data = null;

		if (payload instanceof ByteBuffer byteBuffer) {
			data = byteBuffer;
			if (this.embeddedHeadersMapper != null) {
				messageToEmbed = new MutableMessage<>(data.array(), messageHeaders);
			}
		}
		else {
			byte[] bytes = (byte[]) (payload instanceof byte[] ? payload
					: this.messageConverter.fromMessage(message, byte[].class));
			Assert.notNull(bytes, "payload cannot be null");
			if (this.embeddedHeadersMapper != null) {
				messageToEmbed = new MutableMessage<>(bytes, messageHeaders);
			}
			else {
				data = ByteBuffer.wrap(bytes);
			}
		}

		if (messageToEmbed != null) {
			try {
				byte[] bytes = this.embeddedHeadersMapper.fromMessage(messageToEmbed);
				Assert.notNull(bytes, "payload cannot be null");
				data = ByteBuffer.wrap(bytes);
			}
			catch (Exception ex) {
				throw new MessageConversionException(message, "Cannot embedded headers to payload", ex);
			}
		}

		return new UserRecord().withStreamName(stream).withPartitionKey(partitionKey)
				.withExplicitHashKey(explicitHashKey).withData(data).withSchema(schema);
	}

	private CompletableFuture<UserRecordResult> handleMessageToAws(UserRecord userRecord) {
		try {
			return handleUserRecord(userRecord);
		}
		finally {
			if (this.flushDuration.toMillis() <= 0) {
				this.kinesisProducer.flush();
			}
		}
	}

	private CompletableFuture<UserRecordResult> handleUserRecord(UserRecord userRecord) {
		if (this.backPressureThreshold > 0) {
			var numberOfRecordsInFlight = this.kinesisProducer.getOutstandingRecordsCount();
			if (numberOfRecordsInFlight > this.backPressureThreshold) {
				throw new KplBackpressureException("Cannot send record to Kinesis since buffer is at max capacity.",
						userRecord);
			}
		}

		return listenableFutureToCompletableFuture(this.kinesisProducer.addUserRecord(userRecord));
	}

	private Message<?> handleResponse(Message<?> message, UserRecordResult response, Throwable cause) {
		if (cause != null) {
			throw new MessageHandlingException(message, cause);
		}
		return getMessageBuilderFactory().fromMessage(message).copyHeadersIfAbsent(additionalOnSuccessHeaders(response))
				.build();
	}

	private static Map<String, ?> additionalOnSuccessHeaders(UserRecordResult response) {
		return Map.of(KinesisHeaders.SHARD, response.getShardId(), KinesisHeaders.SEQUENCE_NUMBER,
				response.getSequenceNumber());
	}

	private static <T> CompletableFuture<T> listenableFutureToCompletableFuture(ListenableFuture<T> listenableFuture) {
		CompletableFuture<T> completable = new CompletableFuture<>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				// propagate cancel to the listenable future
				boolean result = listenableFuture.cancel(mayInterruptIfRunning);
				super.cancel(mayInterruptIfRunning);
				return result;
			}

		};

		// add callback
		Futures.addCallback(listenableFuture, new FutureCallback<>() {

			@Override
			public void onSuccess(T result) {
				completable.complete(result);
			}

			@Override
			public void onFailure(Throwable ex) {
				completable.completeExceptionally(ex);
			}

		}, MoreExecutors.directExecutor());

		return completable;
	}

}
