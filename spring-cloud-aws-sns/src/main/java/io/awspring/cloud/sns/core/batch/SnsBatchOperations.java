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

import java.util.Collection;
import java.util.Map;

import io.awspring.cloud.sns.core.SnsNotification;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Provides methods for sending multiple messages to SNS topics in a single batch operation, which is more efficient
 * than sending messages individually.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public interface SnsBatchOperations {

	/**
	 * Sends a batch of messages to the specified SNS topic. Due AWS SNS Batch Api limit. It will split messages in
	 * groups of 10.
	 * 
	 * The result contains information about both successful and failed messages.
	 *
	 * @param topicName The logical name of the SNS topic
	 * @param messages Collection of Spring messages to send
	 * @param <T> The type of the message payload
	 * @return BatchResult containing successful results and errors if there are any
	 */
	<T> BatchResult sendBatch(String topicName, Collection<Message<T>> messages);

	/**
	 * Converts a collection of POJOs to Spring messages and sends them as a batch to the specified SNS topic.
	 * Batch API limit, messages will be split into groups of 10.
	 * <p>
	 * The result contains information about both successful and failed messages.
	 *
	 * @param topicName The logical name of the SNS topic
	 * @param payloads Collection of payloads to convert and send
	 * @param <T> The type of the payload
	 * @return BatchResult containing successful results and errors if there are any
	 */
	<T> BatchResult convertAndSend(String topicName, Collection<T> payloads);


	/**
	 * Converts a collection of notifications to Spring messages and sends them as a batch to the specified SNS topic.
	 * Batch API limit, messages will be split into groups of 10.
	 * <p>
	 * The result contains information about both successful and failed messages.
	 *
	 * @param topicName The logical name of the SNS topic
	 * @param notifications Collection of {@link SnsNotification} to convert and send
	 * @param <T> The type of the payload
	 * @return BatchResult containing successful results and errors if there are any
	 */
	<T> BatchResult sendBatchNotifications(String topicName, Collection<SnsNotification<T>> notifications);





}
