/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Alain Sahli
 */
@SpringApplicationConfiguration(classes = BootQueueListenerTest.QueueListenerTestConfiguration.class)
@IntegrationTest
public class BootQueueListenerTest extends QueueListenerTest {

	@Configuration
	@EnableAutoConfiguration
	@PropertySource({"classpath:Integration-test-config.properties", "file://${els.config.dir}/access.properties"})
	protected static class QueueListenerTestConfiguration {

		@Bean
		public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			return new SimpleMessageListenerContainerFactory();
		}

		@Bean
		public QueueMessagingTemplate queueMessagingTemplate(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver, SimpleMessageListenerContainerFactory factory) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			factory.setSendToMessageTemplate(queueMessagingTemplate);

			return queueMessagingTemplate;
		}

		@Bean
		public MessageListener messageListener() {
			return new MessageListener();
		}

		@Bean
		public MessageListenerWithSendTo messageListenerWithSendTo() {
			return new MessageListenerWithSendTo();
		}

	}

}
