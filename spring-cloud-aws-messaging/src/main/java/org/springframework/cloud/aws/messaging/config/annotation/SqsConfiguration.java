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

package org.springframework.cloud.aws.messaging.config.annotation;

import java.util.Arrays;

import com.amazonaws.services.sqs.AmazonSQSAsync;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.CollectionUtils;

/**
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
public class SqsConfiguration {

	@Autowired(required = false)
	// @checkstyle:off
	private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory();

	// @checkstyle:on
	@Autowired(required = false)
	private final QueueMessageHandlerFactory queueMessageHandlerFactory = new QueueMessageHandlerFactory();

	// @checkstyle:off
	@Autowired
	public BeanFactory beanFactory;

	// @checkstyle:on
	@Autowired(required = false)
	private ResourceIdResolver resourceIdResolver;

	@Autowired(required = false)
	private MappingJackson2MessageConverter mappingJackson2MessageConverter;

	@Bean
	public SimpleMessageListenerContainer simpleMessageListenerContainer(
			AmazonSQSAsync amazonSqs) {
		if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
			this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
		}
		if (this.simpleMessageListenerContainerFactory.getResourceIdResolver() == null) {
			this.simpleMessageListenerContainerFactory
					.setResourceIdResolver(this.resourceIdResolver);
		}

		SimpleMessageListenerContainer simpleMessageListenerContainer = this.simpleMessageListenerContainerFactory
				.createSimpleMessageListenerContainer();
		simpleMessageListenerContainer.setMessageHandler(queueMessageHandler(amazonSqs));
		return simpleMessageListenerContainer;
	}

	@Bean
	public QueueMessageHandler queueMessageHandler(AmazonSQSAsync amazonSqs) {
		if (this.simpleMessageListenerContainerFactory.getQueueMessageHandler() != null) {
			return this.simpleMessageListenerContainerFactory.getQueueMessageHandler();
		}
		else {
			return getMessageHandler(amazonSqs);
		}
	}

	private QueueMessageHandler getMessageHandler(AmazonSQSAsync amazonSqs) {
		if (this.queueMessageHandlerFactory.getAmazonSqs() == null) {
			this.queueMessageHandlerFactory.setAmazonSqs(amazonSqs);
		}

		if (CollectionUtils
				.isEmpty(this.queueMessageHandlerFactory.getMessageConverters())
				&& this.mappingJackson2MessageConverter != null) {
			this.queueMessageHandlerFactory.setMessageConverters(
					Arrays.asList(this.mappingJackson2MessageConverter));
		}

		this.queueMessageHandlerFactory.setBeanFactory(this.beanFactory);

		return this.queueMessageHandlerFactory.createQueueMessageHandler();
	}

}
