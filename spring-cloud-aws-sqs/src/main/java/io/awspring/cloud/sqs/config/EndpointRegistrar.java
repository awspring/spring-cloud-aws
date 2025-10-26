/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import tools.jackson.databind.json.JsonMapper;

/**
 * Processes the registered {@link Endpoint} instances using the appropriate {@link MessageListenerContainerFactory}.
 * Contains configurations that will be applied to all {@link io.awspring.cloud.sqs.annotation.SqsListener @SqsListener}
 * containers. Such configurations can be set by declaring {@link SqsListenerConfigurer} beans.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see SqsListenerConfigurer
 */
public class EndpointRegistrar implements BeanFactoryAware, SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(EndpointRegistrar.class);

	public static final String DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "defaultSqsListenerContainerFactory";

	private BeanFactory beanFactory;

	private MessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();

	private MessageListenerContainerRegistry listenerContainerRegistry;

	@Nullable
	private String messageListenerContainerRegistryBeanName;

	private String defaultListenerContainerFactoryBeanName = DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	private final Collection<Endpoint> endpoints = new ArrayList<>();

	private Consumer<List<MessageConverter>> messageConvertersConsumer = converters -> {
	};

	private Consumer<List<HandlerMethodArgumentResolver>> methodArgumentResolversConsumer = resolvers -> {
	};

	@Nullable
	private JsonMapper jsonMapper;

	@Nullable
	private Validator validator;

	/**
	 * Set a custom {@link MessageHandlerMethodFactory} implementation.
	 * @param messageHandlerMethodFactory the instance.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		Assert.notNull(messageHandlerMethodFactory, "messageHandlerMethodFactory cannot be null");
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * Set a custom {@link MessageListenerContainerRegistry}.
	 * @param listenerContainerRegistry the instance.
	 */
	public void setListenerContainerRegistry(MessageListenerContainerRegistry listenerContainerRegistry) {
		Assert.notNull(listenerContainerRegistry, "listenerContainerRegistry cannot be null");
		this.listenerContainerRegistry = listenerContainerRegistry;
	}

	/**
	 * Set the bean name for the default {@link MessageListenerContainerFactory}.
	 * @param defaultListenerContainerFactoryBeanName the bean name.
	 */
	public void setDefaultListenerContainerFactoryBeanName(String defaultListenerContainerFactoryBeanName) {
		Assert.isTrue(StringUtils.hasText(defaultListenerContainerFactoryBeanName),
				"defaultListenerContainerFactoryBeanName must have text");
		this.defaultListenerContainerFactoryBeanName = defaultListenerContainerFactoryBeanName;
	}

	/**
	 * Set the bean name for the {@link MessageListenerContainerRegistry}.
	 * @param messageListenerContainerRegistryBeanName the bean name.
	 */
	public void setMessageListenerContainerRegistryBeanName(String messageListenerContainerRegistryBeanName) {
		Assert.isTrue(StringUtils.hasText(messageListenerContainerRegistryBeanName),
				"messageListenerContainerRegistryBeanName must have text");
		this.messageListenerContainerRegistryBeanName = messageListenerContainerRegistryBeanName;
	}

	/**
	 * Set the object mapper to be used to deserialize payloads fot SqsListener endpoints.
	 * @param jsonMapper the object mapper instance.
	 */
	public void setJsonMapper(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "objectMapper cannot be null.");
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Set the {@link Validator} instance used for payload validating in {@link HandlerMethodArgumentResolver}
	 * instances.
	 * @param validator payload validator.
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Manage the list of {@link MessageConverter} instances to be used to convert payloads.
	 * @param convertersConsumer a consumer for the converters list.
	 */
	public void manageMessageConverters(Consumer<List<MessageConverter>> convertersConsumer) {
		Assert.notNull(convertersConsumer, "convertersConsumer cannot be null");
		this.messageConvertersConsumer = convertersConsumer;
	}

	/**
	 * Manage the list of {@link HandlerMethodArgumentResolver} instances to be used for resolving method arguments.
	 * @param resolversConsumer a consumer for the resolvers list.
	 */
	public void manageMethodArgumentResolvers(Consumer<List<HandlerMethodArgumentResolver>> resolversConsumer) {
		Assert.notNull(resolversConsumer, "resolversConsumer cannot be null");
		this.methodArgumentResolversConsumer = resolversConsumer;
	}

	/**
	 * Get the message converters list consumer.
	 * @return the consumer.
	 */
	public Consumer<List<MessageConverter>> getMessageConverterConsumer() {
		return this.messageConvertersConsumer;
	}

	/**
	 * Get the method argument resolvers list consumer.
	 * @return the consumer.
	 */
	public Consumer<List<HandlerMethodArgumentResolver>> getMethodArgumentResolversConsumer() {
		return this.methodArgumentResolversConsumer;
	}

	/**
	 * Get the object mapper used to deserialize payloads.
	 * @return the object mapper instance.
	 */
	@Nullable
	public JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	/**
	 * Return the {@link MessageHandlerMethodFactory} to be used to create {@link MessageHandler} instances for the
	 * {@link Endpoint}s.
	 * @return the factory instance.
	 */
	public MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
		return this.messageHandlerMethodFactory;
	}

	/**
	 * Return the {@link Validator} instance used for payload validating in {@link HandlerMethodArgumentResolver}
	 * instances.
	 * @return the payload validator.
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Register an {@link Endpoint} within this registrar for later processing.
	 * @param endpoint the endpoint.
	 */
	public void registerEndpoint(Endpoint endpoint) {
		this.endpoints.add(endpoint);
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (this.listenerContainerRegistry == null) {
			Assert.hasText(this.messageListenerContainerRegistryBeanName,
					"messageListenerContainerRegistryBeanName not set");
			this.listenerContainerRegistry = beanFactory.getBean(this.messageListenerContainerRegistryBeanName,
					MessageListenerContainerRegistry.class);
		}
		this.endpoints.forEach(this::process);
	}

	private void process(Endpoint endpoint) {
		logger.debug("Processing endpoint {}", endpoint.getId());
		this.listenerContainerRegistry.registerListenerContainer(createContainerFor(endpoint));
	}

	private MessageListenerContainer<?> createContainerFor(Endpoint endpoint) {
		String factoryBeanName = getListenerContainerFactoryName(endpoint);
		Assert.isTrue(this.beanFactory.containsBean(factoryBeanName),
				() -> "No MessageListenerContainerFactory bean with name " + factoryBeanName
						+ " found for endpoint names " + endpoint.getLogicalNames());
		return this.beanFactory.getBean(factoryBeanName, MessageListenerContainerFactory.class)
				.createContainer(endpoint);
	}

	private String getListenerContainerFactoryName(Endpoint endpoint) {
		return StringUtils.hasText(endpoint.getListenerContainerFactoryName())
				? endpoint.getListenerContainerFactoryName()
				: this.defaultListenerContainerFactoryBeanName;
	}

}
