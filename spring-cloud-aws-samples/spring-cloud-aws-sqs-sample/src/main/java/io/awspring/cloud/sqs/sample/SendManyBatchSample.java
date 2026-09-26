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
package io.awspring.cloud.sqs.sample;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Sample demonstrating {@link SqsTemplate#sendMany} sending more than 10 messages at once. The template automatically
 * partitions the messages into batches of 10 and sends them in parallel (for standard queues) or sequentially per
 * message group (for FIFO queues).
 *
 * @author José Iêdo
 */
@Configuration
public class SendManyBatchSample {

	private static final Logger LOGGER = LoggerFactory.getLogger(SendManyBatchSample.class);

	private static final String QUEUE_NAME = "send-many-batch-queue";

	@SqsListener(queueNames = QUEUE_NAME, maxMessagesPerPoll = "25", maxConcurrentMessages = "25")
	void listen(List<Message<String>> messages) {
		LOGGER.info("Received {} messages: {}", messages.size(), messages.stream().map(Message::getPayload).toList());
	}

	@Bean
	public ApplicationRunner sendManyMessages(SqsTemplate sqsTemplate) {
		return args -> {
			List<Message<String>> messages = IntStream.range(0, 25).mapToObj(index -> "Message-" + index)
					.map(payload -> MessageBuilder.withPayload(payload).build()).toList();
			LOGGER.info("Sending {} messages to queue {}", messages.size(), QUEUE_NAME);
			SendResult.Batch<String> result = sqsTemplate.sendMany(QUEUE_NAME, messages);
			LOGGER.info("Sent successfully: {}, failed: {}", result.successful().size(), result.failed().size());
		};
	}

}
