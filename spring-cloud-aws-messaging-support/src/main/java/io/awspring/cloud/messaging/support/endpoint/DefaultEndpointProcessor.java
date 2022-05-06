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
package io.awspring.cloud.messaging.support.endpoint;

import io.awspring.cloud.messaging.support.config.MessageListenerContainerFactory;
import io.awspring.cloud.messaging.support.config.MessagingConfigUtils;
import io.awspring.cloud.messaging.support.listener.MessageListenerContainer;
import io.awspring.cloud.messaging.support.listener.MessageListenerContainerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class DefaultEndpointProcessor implements EndpointProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(DefaultEndpointProcessor.class);

	public static final String DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "defaultListenerContainerFactory";

	public static final String ENDPOINT_REGISTRY_BEAN_NAME = "defaultEndpointRegistry";

	private BeanFactory beanFactory;

	private MessageListenerContainerRegistry listenerContainerRegistry;

	@Override
	public void afterSingletonsInstantiated() {
		Assert.isTrue(beanFactory.containsBean(MessagingConfigUtils.ENDPOINT_REGISTRY_BEAN_NAME),
				() -> "A MessageListenerContainerRegistry implementation must be registered with name "
						+ MessagingConfigUtils.ENDPOINT_REGISTRY_BEAN_NAME);
		this.listenerContainerRegistry = beanFactory.getBean(
				MessagingConfigUtils.MESSAGE_LISTENER_CONTAINER_REGISTRY_BEAN_NAME,
				MessageListenerContainerRegistry.class);
		this.beanFactory.getBean(MessagingConfigUtils.ENDPOINT_REGISTRY_BEAN_NAME, EndpointRegistry.class)
				.retrieveEndpoints().forEach(this::process);
	}

	@Override
	public void process(Endpoint endpoint) {
		logger.debug("Processing endpoint: " + endpoint);
		this.listenerContainerRegistry.registerListenerContainer(createContainerFor(endpoint));
	}

	@SuppressWarnings("unchecked")
	private MessageListenerContainer<?> createContainerFor(Endpoint endpoint) {
		String factoryBeanName = getListenerContainerFactoryName(endpoint);
		Assert.isTrue(this.beanFactory.containsBean(factoryBeanName),
				() -> "No bean with name " + factoryBeanName + " found for MessageListenerContainerFactory.");
		return this.beanFactory.getBean(factoryBeanName, MessageListenerContainerFactory.class).create(endpoint);
	}

	private String getListenerContainerFactoryName(Endpoint endpoint) {
		return StringUtils.hasText(endpoint.getListenerContainerFactoryName())
				? endpoint.getListenerContainerFactoryName()
				: MessagingConfigUtils.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
