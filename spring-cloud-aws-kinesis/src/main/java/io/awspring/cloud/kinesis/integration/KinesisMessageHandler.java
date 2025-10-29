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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.KinesisRequest;
import software.amazon.awssdk.services.kinesis.model.KinesisResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;

/**
 * The {@link AbstractMessageHandler} implementation for the Amazon Kinesis {@code putRecord(s)}.
 *
 * @author Artem Bilan
 * @author Jacob Severson
 *
 * @since 4.0
 *
 * @see KinesisAsyncClient#putRecord(PutRecordRequest)
 * @see KinesisAsyncClient#putRecords(PutRecordsRequest)
 */
public class KinesisMessageHandler extends AbstractMessageProducingHandler {

	private static final long DEFAULT_SEND_TIMEOUT = 10000;

	private final KinesisAsyncClient amazonKinesis;

	private MessageConverter messageConverter = new ConvertingFromMessageConverter(new SerializingConverter());

	private Expression streamExpression;

	private Expression partitionKeyExpression;

	private Expression explicitHashKeyExpression;

	private Expression sequenceNumberExpression;

	private OutboundMessageMapper<byte[]> embeddedHeadersMapper;

	private EvaluationContext evaluationContext;

	private Expression sendTimeoutExpression = new ValueExpression<>(DEFAULT_SEND_TIMEOUT);

	public KinesisMessageHandler(KinesisAsyncClient amazonKinesis) {
		Assert.notNull(amazonKinesis, "'amazonKinesis' must not be null.");
		this.amazonKinesis = amazonKinesis;
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

	public void setSequenceNumberExpressionString(String sequenceNumberExpression) {
		setSequenceNumberExpression(EXPRESSION_PARSER.parseExpression(sequenceNumberExpression));
	}

	public void setSequenceNumberExpression(Expression sequenceNumberExpression) {
		this.sequenceNumberExpression = sequenceNumberExpression;
	}

	/**
	 * Specify a {@link OutboundMessageMapper} for embedding message headers into the record data together with payload.
	 * @param embeddedHeadersMapper the {@link OutboundMessageMapper} to embed headers into the record data.
	 * @see org.springframework.integration.support.json.EmbeddedHeadersJsonMessageMapper
	 */
	public void setEmbeddedHeadersMapper(OutboundMessageMapper<byte[]> embeddedHeadersMapper) {
		this.embeddedHeadersMapper = embeddedHeadersMapper;
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
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		KinesisRequest request = messageToAwsRequest(message);
		CompletableFuture<?> resultFuture = handleMessageToAws(request)
				.handle((response, ex) -> handleResponse(message, request, response, ex));

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
			throw new MessageTimeoutException(message, "Timeout waiting for response from AmazonKinesis", te);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
		catch (ExecutionException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected Message<?> handleResponse(Message<?> message, KinesisRequest request, KinesisResponse response,
			Throwable cause) {

		if (cause != null) {
			throw new MessageHandlingException(message, cause);
		}
		return getMessageBuilderFactory().fromMessage(message)
				.copyHeadersIfAbsent(additionalOnSuccessHeaders(request, response)).build();
	}

	private KinesisRequest messageToAwsRequest(Message<?> message) {
		if (message.getPayload() instanceof PutRecordsRequest putRecordsRequest) {
			return putRecordsRequest;
		}
		else {
			return message.getPayload() instanceof PutRecordRequest putRecordRequest ? putRecordRequest
					: buildPutRecordRequest(message);
		}
	}

	private PutRecordRequest buildPutRecordRequest(Message<?> message) {
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
		Assert.state(partitionKey != null, "'partitionKey' must not be null for sending a Kinesis record. "
				+ "Consider configuring this handler with a 'partitionKey'( or 'partitionKeyExpression') or supply an "
				+ "'aws_partitionKey' message header.");

		String explicitHashKey = (this.explicitHashKeyExpression != null
				? this.explicitHashKeyExpression.getValue(this.evaluationContext, message, String.class)
				: null);

		String sequenceNumber = messageHeaders.get(KinesisHeaders.SEQUENCE_NUMBER, String.class);
		if (!StringUtils.hasText(sequenceNumber) && this.sequenceNumberExpression != null) {
			sequenceNumber = this.sequenceNumberExpression.getValue(this.evaluationContext, message, String.class);
		}

		Object payload = message.getPayload();

		SdkBytes data = null;

		Message<?> messageToEmbed = null;

		if (payload instanceof ByteBuffer byteBuffer) {
			data = SdkBytes.fromByteBuffer(byteBuffer);
			if (this.embeddedHeadersMapper != null) {
				messageToEmbed = new MutableMessage<>(data.asByteArray(), messageHeaders);
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
				data = SdkBytes.fromByteArray(bytes);
			}
		}

		if (messageToEmbed != null) {
			try {
				byte[] bytes = this.embeddedHeadersMapper.fromMessage(messageToEmbed);
				Assert.notNull(bytes, "payload cannot be null");
				data = SdkBytes.fromByteArray(bytes);
			}
			catch (Exception ex) {
				throw new MessageConversionException(message, "Cannot embedded headers to payload", ex);
			}
		}

		return PutRecordRequest.builder().streamName(stream).partitionKey(partitionKey).explicitHashKey(explicitHashKey)
				.sequenceNumberForOrdering(sequenceNumber).data(data).build();
	}

	private CompletableFuture<? extends KinesisResponse> handleMessageToAws(KinesisRequest request) {
		if (request instanceof PutRecordsRequest putRecordsRequest) {
			return this.amazonKinesis.putRecords(putRecordsRequest);
		}
		else {
			return this.amazonKinesis.putRecord((PutRecordRequest) request);
		}
	}

	protected Map<String, ?> additionalOnSuccessHeaders(KinesisRequest request, KinesisResponse response) {
		if (response instanceof PutRecordResponse putRecordResponse) {
			return Map.of(KinesisHeaders.SHARD, putRecordResponse.shardId(), KinesisHeaders.SEQUENCE_NUMBER,
					putRecordResponse.sequenceNumber(), KinesisHeaders.SERVICE_RESULT, response);
		}
		return Map.of(KinesisHeaders.SERVICE_RESULT, response);
	}

}
