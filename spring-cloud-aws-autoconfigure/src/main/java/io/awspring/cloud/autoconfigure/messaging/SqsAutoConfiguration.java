/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.autoconfigure.messaging;

import java.util.Arrays;
import java.util.Optional;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.context.annotation.ConditionalOnMissingAmazonClient;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.env.ResourceIdResolver;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.listener.QueueMessageHandler;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.CollectionUtils;

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SQS integration.
 *
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 */
@ConditionalOnClass(SimpleMessageListenerContainer.class)
@ConditionalOnMissingBean(SimpleMessageListenerContainer.class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SqsProperties.class)
public class SqsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingAmazonClient(AmazonSQS.class)
	static class SqsClientConfiguration {

		private final AWSCredentialsProvider awsCredentialsProvider;

		private final RegionProvider regionProvider;

		private final SqsProperties properties;

		private final ClientConfiguration clientConfiguration;

		SqsClientConfiguration(ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
				ObjectProvider<RegionProvider> regionProvider, SqsProperties properties,
				@Qualifier(GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME) ObjectProvider<ClientConfiguration> globalClientConfiguration,
				@Qualifier("sqsClientConfiguration") ObjectProvider<ClientConfiguration> sqsClientConfiguration) {
			this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
			this.regionProvider = properties.getRegion() == null ? regionProvider.getIfAvailable()
					: new StaticRegionProvider(properties.getRegion());
			this.properties = properties;
			this.clientConfiguration = sqsClientConfiguration.getIfAvailable(globalClientConfiguration::getIfAvailable);
		}

		@Lazy
		@Bean(destroyMethod = "shutdown")
		public AmazonSQSBufferedAsyncClient amazonSQS() throws Exception {
			AmazonWebserviceClientFactoryBean<AmazonSQSAsyncClient> clientFactoryBean = new AmazonWebserviceClientFactoryBean<>(
					AmazonSQSAsyncClient.class, this.awsCredentialsProvider, this.regionProvider,
					this.clientConfiguration);
			Optional.ofNullable(properties.getEndpoint()).ifPresent(clientFactoryBean::setCustomEndpoint);
			clientFactoryBean.afterPropertiesSet();
			return new AmazonSQSBufferedAsyncClient(clientFactoryBean.getObject());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SqsConfiguration {

		private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory;

		private final QueueMessageHandlerFactory queueMessageHandlerFactory;

		private final BeanFactory beanFactory;

		private final ResourceIdResolver resourceIdResolver;

		private final MappingJackson2MessageConverter mappingJackson2MessageConverter;

		private final ObjectMapper objectMapper;

		SqsConfiguration(ObjectProvider<SimpleMessageListenerContainerFactory> simpleMessageListenerContainerFactory,
				ObjectProvider<QueueMessageHandlerFactory> queueMessageHandlerFactory, BeanFactory beanFactory,
				ObjectProvider<ResourceIdResolver> resourceIdResolver,
				ObjectProvider<MappingJackson2MessageConverter> mappingJackson2MessageConverter,
				ObjectProvider<ObjectMapper> objectMapper, SqsProperties sqsProperties) {
			this.simpleMessageListenerContainerFactory = simpleMessageListenerContainerFactory
					.getIfAvailable(() -> createSimpleMessageListenerContainerFactory(sqsProperties));
			this.queueMessageHandlerFactory = queueMessageHandlerFactory
					.getIfAvailable(() -> createQueueMessageHandlerFactory(sqsProperties));
			this.beanFactory = beanFactory;
			this.resourceIdResolver = resourceIdResolver.getIfAvailable();
			this.mappingJackson2MessageConverter = mappingJackson2MessageConverter.getIfAvailable();
			this.objectMapper = objectMapper.getIfAvailable();
		}

		private static QueueMessageHandlerFactory createQueueMessageHandlerFactory(SqsProperties sqsProperties) {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();

			Optional.ofNullable(sqsProperties.getHandler().getDefaultDeletionPolicy())
					.ifPresent(factory::setSqsMessageDeletionPolicy);

			return factory;
		}

		private static SimpleMessageListenerContainerFactory createSimpleMessageListenerContainerFactory(
				SqsProperties sqsProperties) {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();

			Optional.ofNullable(sqsProperties.getListener().getBackOffTime()).ifPresent(factory::setBackOffTime);
			Optional.ofNullable(sqsProperties.getListener().getMaxNumberOfMessages())
					.ifPresent(factory::setMaxNumberOfMessages);
			Optional.ofNullable(sqsProperties.getListener().getQueueStopTimeout())
					.ifPresent(factory::setQueueStopTimeout);
			Optional.ofNullable(sqsProperties.getListener().getVisibilityTimeout())
					.ifPresent(factory::setVisibilityTimeout);
			Optional.ofNullable(sqsProperties.getListener().getWaitTimeout()).ifPresent(factory::setWaitTimeOut);
			factory.setAutoStartup(sqsProperties.getListener().isAutoStartup());

			return factory;
		}

		@Bean
		public SimpleMessageListenerContainer simpleMessageListenerContainer(AmazonSQSAsync amazonSqs,
				QueueMessageHandler queueMessageHandler) {
			if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
				this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
			}
			if (this.simpleMessageListenerContainerFactory.getResourceIdResolver() == null
					&& this.resourceIdResolver != null) {
				this.simpleMessageListenerContainerFactory.setResourceIdResolver(this.resourceIdResolver);
			}

			SimpleMessageListenerContainer simpleMessageListenerContainer = this.simpleMessageListenerContainerFactory
					.createSimpleMessageListenerContainer();

			simpleMessageListenerContainer.setMessageHandler(queueMessageHandler);
			return simpleMessageListenerContainer;
		}

		@Bean
		@ConditionalOnMissingBean(QueueMessagingTemplate.class)
		public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSqs,
				@Autowired(required = false) ObjectMapper objectMapper) {
			if (objectMapper != null) {
				return new QueueMessagingTemplate(amazonSqs, resourceIdResolver, objectMapper);
			}
			else {
				return new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			}
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

			if (CollectionUtils.isEmpty(this.queueMessageHandlerFactory.getMessageConverters())
					&& this.mappingJackson2MessageConverter != null) {
				this.queueMessageHandlerFactory
						.setMessageConverters(Arrays.asList(this.mappingJackson2MessageConverter));
			}

			this.queueMessageHandlerFactory.setBeanFactory(this.beanFactory);
			this.queueMessageHandlerFactory.setObjectMapper(this.objectMapper);

			return this.queueMessageHandlerFactory.createQueueMessageHandler();
		}

	}

}
