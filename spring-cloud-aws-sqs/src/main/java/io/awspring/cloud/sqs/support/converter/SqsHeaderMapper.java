package io.awspring.cloud.sqs.support.converter;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.SqsAcknowledge;
import io.awspring.cloud.sqs.support.converter.context.ContextAwareHeaderMapper;
import io.awspring.cloud.sqs.support.converter.context.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.context.SqsMessageConversionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageHeaderAccessor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsHeaderMapper implements ContextAwareHeaderMapper<Message> {

	private static final Logger logger = LoggerFactory.getLogger(SqsHeaderMapper.class);

	@Override
	public void fromHeaders(MessageHeaders headers, Message target) {
		// We'll probably use this for SqsTemplate later
	}

	@Override
	public MessageHeaders toHeaders(Message source) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeadersIfAbsent(getMessageSystemAttributesAsHeaders(source));
		accessor.copyHeadersIfAbsent(getMessageAttributesAsHeaders(source));
		accessor.setHeader(SqsHeaders.SQS_MESSAGE_ID_HEADER, source.messageId());
		accessor.setHeader(SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, source.receiptHandle());
		accessor.setHeader(SqsHeaders.SQS_SOURCE_DATA_HEADER, source);
		accessor.setHeader(SqsHeaders.SQS_RECEIVED_AT_HEADER, Instant.now());
		return accessor.getMessageHeaders();
	}

	private Map<String, String> getMessageAttributesAsHeaders(Message source) {
		return source
			.messageAttributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> SqsHeaders.SQS_MA_HEADER_PREFIX + entry.getKey(), entry -> entry.getValue().stringValue()));
	}

	private Map<String, String> getMessageSystemAttributesAsHeaders(Message source) {
		return source
			.attributes()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(entry -> SqsHeaders.MessageSystemAttribute.SQS_MSA_HEADER_PREFIX + entry.getKey(), Map.Entry::getValue));
	}

	@Override
	public MessageHeaders getContextHeaders(Message source, MessageConversionContext context) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		ConfigUtils.INSTANCE
			.acceptIfInstance(context, SqsMessageConversionContext.class, sqsContext -> addSqsContextHeaders(source, sqsContext, accessor));
		return accessor.toMessageHeaders();
	}

	private void addSqsContextHeaders(Message source, SqsMessageConversionContext sqsContext, MessageHeaderAccessor accessor) {
		QueueAttributes queueAttributes = sqsContext.getQueueAttributes();
		SqsAsyncClient sqsAsyncClient = sqsContext.getSqsAsyncClient();
		accessor.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, queueAttributes.getQueueName());
		accessor.setHeader(SqsHeaders.SQS_QUEUE_URL_HEADER, queueAttributes.getQueueUrl());
		accessor.setHeader(SqsHeaders.SQS_QUEUE_ATTRIBUTES_HEADER, queueAttributes);
		accessor.setHeader(SqsHeaders.SQS_ACKNOWLEDGMENT_HEADER,
			new SqsAcknowledge(sqsAsyncClient, queueAttributes.getQueueUrl(), source.receiptHandle()));
		accessor.setHeader(SqsHeaders.SQS_VISIBILITY_HEADER,
			new QueueMessageVisibility(sqsAsyncClient, queueAttributes.getQueueUrl(), source.receiptHandle()));
	}
}
