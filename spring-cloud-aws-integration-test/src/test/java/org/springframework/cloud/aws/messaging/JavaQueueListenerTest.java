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

import com.amazonaws.services.sqs.AmazonSQSAsync;

import org.springframework.cloud.aws.IntegrationTestConfig;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Alain Sahli
 */
@ContextConfiguration(classes = JavaQueueListenerTest.JavaQueueListenerTestConfiguration.class)
public class JavaQueueListenerTest extends QueueListenerTest {

	@Configuration
	@EnableSqs
	@Import(IntegrationTestConfig.class)
	protected static class JavaQueueListenerTestConfiguration {

		@Bean
		public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
			factory.setVisibilityTimeout(5);

			return factory;
		}

		@Bean
		public QueueMessageHandlerFactory queueMessageHandlerFactory(
				QueueMessagingTemplate queueMessagingTemplate) {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
			factory.setSendToMessagingTemplate(queueMessagingTemplate);

			return factory;
		}

		@Bean
		public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSqs,
				ResourceIdResolver resourceIdResolver) {
			return new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
		}

		@Bean
		public MessageListener messageListener() {
			return new MessageListener();
		}

		@Bean
		public MessageListenerWithSendTo messageListenerWithSendTo() {
			return new MessageListenerWithSendTo();
		}

		@Bean
		public RedrivePolicyTestListener redrivePolicyTestListener() {
			return new RedrivePolicyTestListener();
		}

		@Bean
		public ManualDeletionPolicyTestListener manualDeletionPolicyTestListener() {
			return new ManualDeletionPolicyTestListener();
		}

	}

}
