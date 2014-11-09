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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Alain Sahli
 */
@Configuration
@Import(DelegatingQueueMessageHandlerConfiguration.class)
public class SqsConfiguration {

	@Autowired(required = false)
	private SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory;

	@Bean
	public SimpleMessageListenerContainer simpleMessageListenerContainer(AmazonSQS amazonSqs, QueueMessageHandler queueMessageHandler) {
		if (this.simpleMessageListenerContainerFactory != null) {
			setMandatoryPropertiesIfNull(amazonSqs, queueMessageHandler);
			return this.simpleMessageListenerContainerFactory.createSimpleMessageListenerContainer();
		} else {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
			factory.setAmazonSqs(amazonSqs);
			factory.setMessageHandler(queueMessageHandler);

			return factory.createSimpleMessageListenerContainer();
		}
	}

	private void setMandatoryPropertiesIfNull(AmazonSQS amazonSqs, QueueMessageHandler queueMessageHandler) {
		if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
			this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
		}
		if (this.simpleMessageListenerContainerFactory.getMessageHandler() == null) {
			this.simpleMessageListenerContainerFactory.setMessageHandler(queueMessageHandler);
		}
	}

}
