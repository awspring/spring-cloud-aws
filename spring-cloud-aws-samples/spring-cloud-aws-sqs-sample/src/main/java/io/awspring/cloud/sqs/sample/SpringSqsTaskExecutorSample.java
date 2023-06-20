/*
 * Copyright 2013-2023 the original author or authors.
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

import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.UUID;
import java.util.concurrent.ThreadFactory;

@Configuration
public class SpringSqsTaskExecutorSample {

	private static final String NEW_PURCHASE_QUEUE = "new-purchase-queue";
	private static final int MAX_CONCURRENT_MESSAGES = 10;
	private static final int TOTAL_NUMBER_QUEUES = 10;
	private static final int MAX_MESSAGES_PER_POLL = 10;

	@Bean
	public SqsAsyncClient sqsAsyncClient() {
		return SqsAsyncClient.builder().build();
	}

	@Bean
	public SqsMessageListenerContainerFactory<Object> sqsMessageListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
		return SqsMessageListenerContainerFactory
			.builder()
			.sqsAsyncClient(sqsAsyncClient)
			.configure(options -> {
				options.componentsTaskExecutor(customTaskExecutor());
			})
			.build();
	}

	private TaskExecutor customTaskExecutor() {
		int poolSize = MAX_CONCURRENT_MESSAGES * TOTAL_NUMBER_QUEUES;
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(MAX_MESSAGES_PER_POLL);
		executor.setMaxPoolSize(poolSize);
		executor.setQueueCapacity(poolSize);
		executor.setAllowCoreThreadTimeOut(true);
		executor.setThreadFactory(customThreadFactory()); //To avoid unnecessary thread hopping between blocking components, a MessageExecutionThreadFactory MUST be set to the executor.
		executor.afterPropertiesSet();
		return executor;
	}

	private ThreadFactory customThreadFactory() {
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		threadFactory.setThreadNamePrefix(UUID.randomUUID() + "-");
		return threadFactory;
	}

	@Bean
	public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
		return SqsTemplate.builder()
			.sqsAsyncClient(sqsAsyncClient)
			.build();
	}

	@SqsListener(NEW_PURCHASE_QUEUE)
	public void listen(Message<Purchase> message) {
		System.out.println(message);
	}

	@Bean
	public ApplicationRunner sendMessageToQueue(SqsTemplate sqsTemplate) {
		return args -> sqsTemplate.sendAsync(NEW_PURCHASE_QUEUE, new Purchase(UUID.randomUUID()));
	}

	public record Purchase(UUID productId) {
	}
}
