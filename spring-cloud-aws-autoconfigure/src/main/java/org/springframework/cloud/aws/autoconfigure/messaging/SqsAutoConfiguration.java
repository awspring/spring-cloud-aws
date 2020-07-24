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

package org.springframework.cloud.aws.autoconfigure.messaging;

import java.util.Arrays;
import java.util.Optional;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.CollectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SQS integration.
 *
 * @author Maciej Walkowiak
 */
@ConditionalOnClass(SimpleMessageListenerContainer.class)
@ConditionalOnMissingBean(SimpleMessageListenerContainer.class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cloud.aws.sqs.enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(SqsProperties.class)
public class SqsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingAmazonClient(AmazonSQS.class)
	static class SqsClientConfiguration {

		private final AWSCredentialsProvider awsCredentialsProvider;

		private final RegionProvider regionProvider;

		SqsClientConfiguration(
				ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
				ObjectProvider<RegionProvider> regionProvider) {
			this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
			this.regionProvider = regionProvider.getIfAvailable();
		}

		@Lazy
		@Bean(destroyMethod = "shutdown")
		public AmazonSQSBufferedAsyncClient amazonSQS() throws Exception {
			AmazonWebserviceClientFactoryBean<AmazonSQSAsyncClient> clientFactoryBean = new AmazonWebserviceClientFactoryBean<>(
					AmazonSQSAsyncClient.class, this.awsCredentialsProvider,
					this.regionProvider);
			clientFactoryBean.afterPropertiesSet();
			return new AmazonSQSBufferedAsyncClient(clientFactoryBean.getObject());
		}

	}

	@Configuration
	static class SqsConfiguration {

		private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory;

		private final QueueMessageHandlerFactory queueMessageHandlerFactory;

		private final BeanFactory beanFactory;

		private final ResourceIdResolver resourceIdResolver;

		private final MappingJackson2MessageConverter mappingJackson2MessageConverter;

		private final ObjectMapper objectMapper;

		private final SqsProperties sqsProperties;

		SqsConfiguration(
				ObjectProvider<SimpleMessageListenerContainerFactory> simpleMessageListenerContainerFactory,
				ObjectProvider<QueueMessageHandlerFactory> queueMessageHandlerFactory,
				BeanFactory beanFactory,
				ObjectProvider<ResourceIdResolver> resourceIdResolver,
				ObjectProvider<MappingJackson2MessageConverter> mappingJackson2MessageConverter,
				ObjectProvider<ObjectMapper> objectMapper, SqsProperties sqsProperties) {
			this.simpleMessageListenerContainerFactory = simpleMessageListenerContainerFactory
					.getIfAvailable(() -> createSimpleMessageListenerContainerFactory(
							sqsProperties));
			this.queueMessageHandlerFactory = queueMessageHandlerFactory.getIfAvailable(
					() -> createQueueMessageHandlerFactory(sqsProperties));
			this.beanFactory = beanFactory;
			this.resourceIdResolver = resourceIdResolver.getIfAvailable();
			this.mappingJackson2MessageConverter = mappingJackson2MessageConverter
					.getIfAvailable();
			this.objectMapper = objectMapper.getIfAvailable();
			this.sqsProperties = sqsProperties;
		}

		private static QueueMessageHandlerFactory createQueueMessageHandlerFactory(
				SqsProperties sqsProperties) {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();

			Optional.ofNullable(sqsProperties.getHandler().getDefaultDeletionPolicy())
					.ifPresent(factory::setSqsMessageDeletionPolicy);

			return factory;
		}

		private static SimpleMessageListenerContainerFactory createSimpleMessageListenerContainerFactory(
				SqsProperties sqsProperties) {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();

			Optional.ofNullable(sqsProperties.getListener().getBackOffTime())
					.ifPresent(factory::setBackOffTime);
			Optional.ofNullable(sqsProperties.getListener().getMaxNumberOfMessages())
					.ifPresent(factory::setMaxNumberOfMessages);
			Optional.ofNullable(sqsProperties.getListener().getQueueStopTimeout())
					.ifPresent(factory::setQueueStopTimeout);
			Optional.ofNullable(sqsProperties.getListener().getVisibilityTimeout())
					.ifPresent(factory::setVisibilityTimeout);
			Optional.ofNullable(sqsProperties.getListener().getWaitTimeout())
					.ifPresent(factory::setWaitTimeOut);
			factory.setAutoStartup(sqsProperties.getListener().isAutoStartup());

			return factory;
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

			simpleMessageListenerContainer
					.setMessageHandler(queueMessageHandler(amazonSqs));
			return simpleMessageListenerContainer;
		}

		@Bean
		public QueueMessageHandler queueMessageHandler(AmazonSQSAsync amazonSqs) {
			if (this.simpleMessageListenerContainerFactory
					.getQueueMessageHandler() != null) {
				return this.simpleMessageListenerContainerFactory
						.getQueueMessageHandler();
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

}
