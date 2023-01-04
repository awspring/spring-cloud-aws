package io.awspring.cloud.sqs.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@Component
public class SqsSampleProducer {

	private final SqsAsyncClient sqsAsyncClient;

	private final ObjectMapper objectMapper;

	public SqsSampleProducer(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.objectMapper = objectMapper;
	}

	public CompletableFuture<Void> sendToUrl(String queueUrl, Object payload) {
		return this.sqsAsyncClient
				.sendMessage(request -> request.messageBody(getMessageBodyAsJson(payload)).queueUrl(queueUrl))
				.thenRun(() -> {
				});
	}

	public CompletableFuture<Void> send(String queueName, Object payload) {
		return this.sqsAsyncClient.getQueueUrl(request -> request.queueName(queueName))
				.thenApply(GetQueueUrlResponse::queueUrl).thenCompose(queueUrl -> sendToUrl(queueUrl, payload));
	}

	private String getMessageBodyAsJson(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Error converting payload: " + payload, e);
		}
	}

}
