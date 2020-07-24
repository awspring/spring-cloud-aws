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

package org.springframework.cloud.aws.messaging.config.annotation;

import java.util.Arrays;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
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
 * @author Eddú Meléndez
 * @since 1.0
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
@Deprecated
public class SqsConfiguration {

	private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory;

	private final QueueMessageHandlerFactory queueMessageHandlerFactory;

	private final BeanFactory beanFactory;

	private final ResourceIdResolver resourceIdResolver;

	private final MappingJackson2MessageConverter mappingJackson2MessageConverter;

	private final ObjectMapper objectMapper;

	public SqsConfiguration(
			ObjectProvider<SimpleMessageListenerContainerFactory> simpleMessageListenerContainerFactory,
			ObjectProvider<QueueMessageHandlerFactory> queueMessageHandlerFactory,
			BeanFactory beanFactory,
			ObjectProvider<ResourceIdResolver> resourceIdResolver,
			ObjectProvider<MappingJackson2MessageConverter> mappingJackson2MessageConverter,
			ObjectProvider<ObjectMapper> objectMapper) {
		this.simpleMessageListenerContainerFactory = simpleMessageListenerContainerFactory
				.getIfAvailable(SimpleMessageListenerContainerFactory::new);
		this.queueMessageHandlerFactory = queueMessageHandlerFactory
				.getIfAvailable(QueueMessageHandlerFactory::new);
		this.beanFactory = beanFactory;
		this.resourceIdResolver = resourceIdResolver.getIfAvailable();
		this.mappingJackson2MessageConverter = mappingJackson2MessageConverter
				.getIfAvailable();
		this.objectMapper = objectMapper.getIfAvailable();
	}

	@Bean
	public SimpleMessageListenerContainer simpleMessageListenerContainer(
			AmazonSQSAsync amazonSqs) {
		if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
			this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
		}
		if (this.simpleMessageListenerContainerFactory.getResourceIdResolver() == null
				&& this.resourceIdResolver != null) {
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
		this.queueMessageHandlerFactory.setObjectMapper(this.objectMapper);

		return this.queueMessageHandlerFactory.createQueueMessageHandler();
	}

}
