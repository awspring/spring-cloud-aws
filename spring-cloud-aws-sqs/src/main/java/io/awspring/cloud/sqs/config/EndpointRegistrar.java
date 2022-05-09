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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.DefaultListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Processes the registered {@link Endpoint} instances using the appropriate {@link MessageListenerContainerFactory} to
 * create {@link MessageListenerContainer} instances, which will then be registered in the
 * {@link MessageListenerContainerRegistry}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class EndpointRegistrar implements BeanFactoryAware, SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(EndpointRegistrar.class);

	public static final String DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "defaultSqsListenerContainerFactory";

	private BeanFactory beanFactory;

	private MessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();

	private MessageListenerContainerRegistry listenerContainerRegistry;

	private String messageListenerContainerRegistryBeanName = SqsBeanNames.ENDPOINT_REGISTRY_BEAN_NAME;

	private String defaultListenerContainerFactoryBeanName = DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	private final Collection<AbstractEndpoint> endpoints = new ArrayList<>();

	private Collection<MessageConverter> messageConverters = new ArrayList<>();

	private ObjectMapper objectMapper;

	private boolean isParallelLifecycleManagement = true;

	/**
	 * Set a custom {@link MessageHandlerMethodFactory} implementation.
	 * @param messageHandlerMethodFactory the instance.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
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
	 * Set a custom {@link MessageListenerContainerRegistry}.
	 * @param listenerContainerRegistry the instance.
	 */
	public void setListenerContainerRegistry(MessageListenerContainerRegistry listenerContainerRegistry) {
		this.listenerContainerRegistry = listenerContainerRegistry;
	}

	/**
	 * Set the bean name for the default {@link MessageListenerContainerFactory}.
	 * @param defaultListenerContainerFactoryBeanName the bean name.
	 */
	public void setDefaultListenerContainerFactoryBeanName(String defaultListenerContainerFactoryBeanName) {
		this.defaultListenerContainerFactoryBeanName = defaultListenerContainerFactoryBeanName;
	}

	/**
	 * Set the bean name for the {@link MessageListenerContainerRegistry}.
	 * @param messageListenerContainerRegistryBeanName the bean name.
	 */
	public void setMessageListenerContainerRegistryBeanName(String messageListenerContainerRegistryBeanName) {
		this.messageListenerContainerRegistryBeanName = messageListenerContainerRegistryBeanName;
	}

	public void setMessageConverters(Collection<MessageConverter> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * Set to false if {@link org.springframework.context.SmartLifecycle} management by the
	 * {@link io.awspring.cloud.sqs.listener.DefaultListenerContainerRegistry} should be made sequentially rather than
	 * in parallel.
	 * @param parallelLifecycleManagement the value.
	 */
	public void setParallelLifecycleManagement(boolean parallelLifecycleManagement) {
		this.isParallelLifecycleManagement = parallelLifecycleManagement;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public <E extends AbstractEndpoint> void registerEndpoint(E endpoint) {
		this.endpoints.add(endpoint);
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (this.listenerContainerRegistry == null) {
			this.listenerContainerRegistry = beanFactory.getBean(this.messageListenerContainerRegistryBeanName,
					MessageListenerContainerRegistry.class);
		}
		ConfigUtils.INSTANCE.acceptIfInstance(this.listenerContainerRegistry, DefaultListenerContainerRegistry.class,
			dlcr -> dlcr.setParallelLifecycleManagement(this.isParallelLifecycleManagement));
		this.endpoints.forEach(this::process);
	}

	private void process(AbstractEndpoint endpoint) {
		logger.debug("Processing endpoint {}", endpoint);
		this.listenerContainerRegistry.registerListenerContainer(createContainerFor(endpoint));
	}

	@SuppressWarnings("unchecked")
	public <E extends AbstractEndpoint> MessageListenerContainer<?> createContainerFor(E endpoint) {
		String factoryBeanName = getListenerContainerFactoryName(endpoint);
		Assert.isTrue(this.beanFactory.containsBean(factoryBeanName),
				() -> "No factory bean with name " + factoryBeanName + " found for endpoint " + endpoint.getId());
		return this.beanFactory.getBean(factoryBeanName, MessageListenerContainerFactory.class)
				.createContainer(endpoint);
	}

	private String getListenerContainerFactoryName(Endpoint endpoint) {
		return StringUtils.hasText(endpoint.getListenerContainerFactoryName())
				? endpoint.getListenerContainerFactoryName()
				: this.defaultListenerContainerFactoryBeanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public Collection<MessageConverter> getMessageConverters() {
		return this.messageConverters;
	}

	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}
}
