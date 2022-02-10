/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.it.messaging;

import java.util.concurrent.ThreadPoolExecutor;

import io.awspring.cloud.v3.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Alain Sahli
 */
@SpringBootTest(classes = BootMessageListenerContainerAwsTest.MessageListenerContainerAwsTestConfiguration.class)
class BootMessageListenerContainerAwsTest extends MessageListenerContainerAwsTest {

	@Configuration
	@EnableAutoConfiguration
	protected static class MessageListenerContainerAwsTestConfiguration {

		@Bean
		public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory();
			simpleMessageListenerContainerFactory.setTaskExecutor(taskExecutor());

			return simpleMessageListenerContainerFactory;
		}

		@Bean
		public AsyncTaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
			threadPoolTaskExecutor.setCorePoolSize(10);
			threadPoolTaskExecutor.setMaxPoolSize(200);
			threadPoolTaskExecutor.setQueueCapacity(0);
			threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

			return threadPoolTaskExecutor;
		}

		@Bean
		public MessageReceiver messageReceiver() {
			return new MessageReceiver();
		}

	}

}
