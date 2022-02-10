package io.awspring.cloud.v3.autoconfigure.messaging;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.v3.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.v3.autoconfigure.sqs.SqsProperties;
import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import io.awspring.cloud.v3.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.v3.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.v3.messaging.listener.QueueMessageHandler;
import io.awspring.cloud.v3.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * {@link EnableAutoConfiguration} for {@link SimpleMessageListenerContainer}.
 *
 * @author Luis Duarte
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SqsClient.class, SqsAsyncClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class,
	RegionProviderAutoConfiguration.class,
	SqsAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class MessagingAutoConfiguration {

	@Configuration
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
						 ObjectProvider<ObjectMapper> objectMapper,
						 SqsProperties sqsProperties) {
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

			Optional.ofNullable(sqsProperties.getHandler())
				.flatMap(handlerProperties -> Optional.ofNullable(handlerProperties.getDefaultDeletionPolicy()))
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
		public SimpleMessageListenerContainer simpleMessageListenerContainer(SqsClient amazonSqs) {
			if (this.simpleMessageListenerContainerFactory.getAmazonSqs() == null) {
				this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
			}
			if (this.simpleMessageListenerContainerFactory.getResourceIdResolver() == null
				&& this.resourceIdResolver != null) {
				this.simpleMessageListenerContainerFactory.setResourceIdResolver(this.resourceIdResolver);
			}

			SimpleMessageListenerContainer simpleMessageListenerContainer = this.simpleMessageListenerContainerFactory
				.createSimpleMessageListenerContainer();

			simpleMessageListenerContainer.setMessageHandler(queueMessageHandler(amazonSqs));
			return simpleMessageListenerContainer;
		}

		@Bean
		public QueueMessageHandler queueMessageHandler(SqsClient amazonSqs) {
			if (this.simpleMessageListenerContainerFactory.getQueueMessageHandler() != null) {
				return this.simpleMessageListenerContainerFactory.getQueueMessageHandler();
			}
			else {
				return getMessageHandler(amazonSqs);
			}
		}

		private QueueMessageHandler getMessageHandler(SqsClient amazonSqs) {
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
