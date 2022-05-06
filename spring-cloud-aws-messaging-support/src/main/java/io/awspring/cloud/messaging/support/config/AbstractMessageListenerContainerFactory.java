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
package io.awspring.cloud.messaging.support.config;

import io.awspring.cloud.messaging.support.MessagingUtils;
import io.awspring.cloud.messaging.support.endpoint.Endpoint;
import io.awspring.cloud.messaging.support.listener.AbstractMessageListenerContainer;
import io.awspring.cloud.messaging.support.listener.AsyncMessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageListenerContainerFactory<T, C extends AbstractMessageListenerContainer<T>, E extends Endpoint>
		implements MessageListenerContainerFactory<C, E>, SmartInitializingSingleton, BeanFactoryAware {

	private static final Integer DEFAULT_THREADPOOL_SIZE = 11;
	private final AbstractFactoryOptions<T, ?> factoryOptions;
	private BeanFactory beanFactory;
	private AsyncMessageListener<T> messageListener;

	protected AbstractMessageListenerContainerFactory(AbstractFactoryOptions<T, ?> factoryOptions) {
		this.factoryOptions = factoryOptions;
	}

	@Override
	public C create(E endpoint) {
		C container = createContainerInstance(endpoint);
		MessagingUtils.INSTANCE.acceptIfNotNull(this.factoryOptions.getErrorHandler(), container::setErrorHandler)
				.acceptIfNotNull(this.factoryOptions.getAckHandler(), container::setAckHandler)
				.acceptIfNotNull(this.factoryOptions.getMessageInterceptor(), container::setMessageInterceptor);
		initializeContainer(container);
		return container;
	}

	protected abstract C createContainerInstance(E endpoint);

	protected void initializeContainer(C container) {
	}

	protected ThreadPoolTaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		int poolSize = this.factoryOptions.getMaxWorkersPerContainer() != null
				? this.factoryOptions.getMaxWorkersPerContainer() + 1
				: DEFAULT_THREADPOOL_SIZE;
		taskExecutor.setMaxPoolSize(poolSize);
		taskExecutor.setCorePoolSize(poolSize);
		return taskExecutor;
	}

	public void setMessageListener(AsyncMessageListener<T> messageListener) {
		Assert.notNull(messageListener, "messageListener cannot be null");
		this.messageListener = messageListener;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected AsyncMessageListener<T> getMessageListener() {
		return this.messageListener;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void afterSingletonsInstantiated() {
		if (this.messageListener == null) {
			Assert.isTrue(this.beanFactory.containsBean(MessagingConfigUtils.MESSAGE_LISTENER_BEAN_NAME),
					"An AsyncMessageListener must be registered with name "
							+ MessagingConfigUtils.MESSAGE_LISTENER_BEAN_NAME);
			this.messageListener = this.beanFactory.getBean(MessagingConfigUtils.MESSAGE_LISTENER_BEAN_NAME,
					AsyncMessageListener.class);
		}
		initializeFactory();
	}

	protected void initializeFactory() {
	}
}
