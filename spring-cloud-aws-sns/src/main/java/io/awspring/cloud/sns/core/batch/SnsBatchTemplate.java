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
package io.awspring.cloud.sns.core.batch;

import io.awspring.cloud.sns.core.TopicArnResolver;
import io.awspring.cloud.sns.core.batch.converter.SnsMessageConverter;
import io.awspring.cloud.sns.core.batch.executor.BatchExecutionStrategy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * This template simplifies batch publishing to SNS topics.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public class SnsBatchTemplate implements SnsBatchOperations {

	private final SnsMessageConverter snsMessageConverter;
	private final BatchExecutionStrategy batchExecutionStrategy;
	private final TopicArnResolver topicArnResolver;

	public SnsBatchTemplate(SnsMessageConverter snsMessageConverter, BatchExecutionStrategy batchExecutionStrategy,
			TopicArnResolver topicArnResolver) {
		Assert.notNull(snsMessageConverter, "SnsMessageConverter cannot be null!");
		Assert.notNull(batchExecutionStrategy, "BatchExecutionStrategy cannot be null!");
		Assert.notNull(topicArnResolver, "TopicArnResolver cannot be null!");

		this.snsMessageConverter = snsMessageConverter;
		this.batchExecutionStrategy = batchExecutionStrategy;
		this.topicArnResolver = topicArnResolver;
	}

	/**
	 * Sends a batch of messages to the specified SNS topic.
	 * <p>
	 * Converts each message using the configured {@link SnsMessageConverter}, resolves the topic ARN, and executes the
	 * batch publish operation using the {@link BatchExecutionStrategy}.
	 *
	 * @param topicName The logical name of the SNS topic
	 * @param messages Collection of Spring messages to send
	 * @param <T> The type of the message payload
	 * @return BatchResult containing successful results and any errors
	 */
	@Override
	public <T> BatchResult sendBatch(String topicName, Collection<Message<T>> messages) {
		var batchList = messages.stream().map(snsMessageConverter::covertMessage).toList();
		return batchExecutionStrategy.send(topicArnResolver.resolveTopicArn(topicName), batchList);
	}

	/**
	 * Converts a collection of payloads to Spring messages and sends them as a batch to the specified SNS topic.
	 *
	 * @param topicName The logical name of the SNS topic
	 * @param payload Collection of payloads to convert and send
	 * @param <T> The type of the payload
	 * @return BatchResult containing successful results and any errors
	 */
	@Override
	public <T> BatchResult convertAndSend(String topicName, Collection<T> payload) {
		var batchList = payload.stream().map(it -> MessageBuilder.withPayload(it).build()).map(snsMessageConverter::covertMessage).collect(Collectors.toList());
		return batchExecutionStrategy.send(topicArnResolver.resolveTopicArn(topicName), batchList);
	}

}
