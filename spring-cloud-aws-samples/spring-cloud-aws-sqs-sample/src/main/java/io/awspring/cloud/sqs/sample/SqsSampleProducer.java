/*
 * Copyright 2013-2022 the original author or authors.
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
