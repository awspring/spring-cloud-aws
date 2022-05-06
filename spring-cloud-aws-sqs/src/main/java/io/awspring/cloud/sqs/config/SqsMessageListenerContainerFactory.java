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

import io.awspring.cloud.messaging.support.MessagingUtils;
import io.awspring.cloud.messaging.support.config.AbstractMessageListenerContainerFactory;
import io.awspring.cloud.messaging.support.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.endpoint.SqsEndpoint;
import io.awspring.cloud.sqs.listener.MessageVisibilityExtenderInterceptor;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsMessageListenerContainerFactory
		extends AbstractMessageListenerContainerFactory<String, SqsMessageListenerContainer, SqsEndpoint> {

	private static final Logger logger = LoggerFactory.getLogger(SqsMessageListenerContainerFactory.class);

	private SqsAsyncClient sqsAsyncClient;

	private final SqsFactoryOptions factoryOptions;

	public SqsMessageListenerContainerFactory() {
		this(SqsFactoryOptions.withOptions());
	}

	public SqsMessageListenerContainerFactory(SqsFactoryOptions factoryOptions) {
		super(factoryOptions);
		this.factoryOptions = factoryOptions;
	}

	@Override
	protected SqsMessageListenerContainer createContainerInstance(SqsEndpoint endpoint) {
		SqsContainerOptions containerOptions = createContainerOptions(endpoint);
		SqsMessageListenerContainer container = new SqsMessageListenerContainer(containerOptions, this.sqsAsyncClient,
				getMessageListener(endpoint), super.createTaskExecutor());
		MessagingUtils.INSTANCE.acceptBothIfNoneNull(containerOptions.getMinTimeToProcess(), container,
				this::addVisibilityExtender);
		return container;
	}

	@SuppressWarnings("unchecked")
	private AsyncMessageListener<String> getMessageListener(SqsEndpoint endpoint) {
		return endpoint.isAsync() && super.getBeanFactory().containsBean(SqsConfigUtils.SQS_ASYNC_CLIENT_BEAN_NAME)
				? getBeanFactory().getBean(SqsConfigUtils.SQS_ASYNC_LISTENER_BEAN_NAME, AsyncMessageListener.class)
				: super.getMessageListener();
	}

	private void addVisibilityExtender(Integer minTimeToProcess, SqsMessageListenerContainer container) {
		MessageVisibilityExtenderInterceptor<String> interceptor = new MessageVisibilityExtenderInterceptor<>(
				this.sqsAsyncClient);
		interceptor.setMinTimeToProcessMessage(minTimeToProcess);
		container.setMessageInterceptor(interceptor);
	}

	protected SqsContainerOptions createContainerOptions(SqsEndpoint endpoint) {
		SqsContainerOptions options = SqsContainerOptions.optionsFor(endpoint);
		MessagingUtils.INSTANCE.acceptFirstNonNull(options::messagesPerProduce, factoryOptions.getMessagesPerPoll())
				.acceptFirstNonNull(options::minTimeToProcess, endpoint.getMinTimeToProcess(),
						factoryOptions.getMinTimeToProcess())
				.acceptFirstNonNull(options::simultaneousProduceCalls, endpoint.getSimultaneousPollsPerQueue(),
						factoryOptions.getSimultaneousPollsPerQueue())
				.acceptFirstNonNull(options::produceTimeout, endpoint.getPollTimeoutSeconds(),
						factoryOptions.getPollTimeoutSeconds());
		return options;
	}

	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	protected void initializeContainer(SqsMessageListenerContainer container) {
		logger.debug("Initializing container {}", container);
		container.afterPropertiesSet();
	}

	@Override
	protected void initializeFactory() {
		if (this.sqsAsyncClient == null) {
			Assert.isTrue(getBeanFactory().containsBean(SqsConfigUtils.SQS_ASYNC_CLIENT_BEAN_NAME),
					"A SqsAsyncClient must be registered with name " + SqsConfigUtils.SQS_ASYNC_CLIENT_BEAN_NAME);
			this.sqsAsyncClient = getBeanFactory().getBean(SqsConfigUtils.SQS_ASYNC_CLIENT_BEAN_NAME,
					SqsAsyncClient.class);
		}

	}
}
