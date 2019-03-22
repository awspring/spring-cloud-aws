/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.cloud.aws.IntegrationTestConfig;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Alain Sahli
 */
@ContextConfiguration(classes = JavaMessageListenerContainerAwsTest.MessageListenerContainerAwsTestConfiguration.class)
public class JavaMessageListenerContainerAwsTest extends MessageListenerContainerAwsTest {

	@Configuration
	@EnableSqs
	@Import(IntegrationTestConfig.class)
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
			threadPoolTaskExecutor.setRejectedExecutionHandler(
					new ThreadPoolExecutor.CallerRunsPolicy());

			return threadPoolTaskExecutor;
		}

		@Bean
		public MessageReceiver messageReceiver() {
			return new MessageReceiver();
		}

	}

}
