package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesAware;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SqsAcknowledgementExecutor<T> implements AcknowledgementExecutor<T>, SqsAsyncClientAware, QueueAttributesAware {

	private static final Logger logger = LoggerFactory.getLogger(SqsAcknowledgementExecutor.class);

    private SqsAsyncClient sqsAsyncClient;
    
    private String queueUrl;

	@Override
	public void setQueueAttributes(QueueAttributes queueAttributes) {
		this.queueUrl = queueAttributes.getQueueUrl();
	}

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	public CompletableFuture<Void> execute(Collection<Message<T>> messagesToAck) {
		try {
			return deleteMessages(messagesToAck);
		}
		catch (Exception e) {
			logger.error("Error acknowledging messages {}", MessageHeaderUtils.getId(messagesToAck), e);
			return CompletableFutures.failedFuture(e);
		}
	}

	private CompletableFuture<Void> deleteMessages(Collection<Message<T>> messagesToAck) {
		logger.trace("Acknowledging messages: {}", MessageHeaderUtils.getId(messagesToAck));
		return this.sqsAsyncClient
			.deleteMessageBatch(createDeleteMessageBatchRequest(messagesToAck))
			.thenRun(() -> {})
			.whenComplete((v, t) -> logAckResult(messagesToAck, t));
	}

	private DeleteMessageBatchRequest createDeleteMessageBatchRequest(Collection<Message<T>> messagesToAck) {
		return DeleteMessageBatchRequest
			.builder()
			.queueUrl(this.queueUrl)
			.entries(messagesToAck.stream().map(this::toDeleteMessageEntry).collect(Collectors.toList()))
			.build();
	}

	private DeleteMessageBatchRequestEntry toDeleteMessageEntry(Message<T> message) {
		return DeleteMessageBatchRequestEntry
			.builder()
			.receiptHandle(MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.SQS_RECEIPT_HANDLE_HEADER))
			.id(UUID.randomUUID().toString())
			.build();
	}

	private void logAckResult(Collection<Message<T>> messagesToAck, Throwable t) {
		if (t != null) {
			logger.error("Error acknowledging messages {}", MessageHeaderUtils.getId(messagesToAck), t);
		}
		else {
			logger.trace("Done acknowledging messages: {}", MessageHeaderUtils.getId(messagesToAck));
		}
	}

}
