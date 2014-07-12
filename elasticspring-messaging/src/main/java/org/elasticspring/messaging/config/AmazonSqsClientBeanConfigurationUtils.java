/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.config;

import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.messaging.support.SuppressingExecutorServiceAdapter;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * @author Alain Sahli
 */
public final class AmazonSqsClientBeanConfigurationUtils {

	// TODO: this constant is only public for testing...
	public static final String AMAZON_BUFFER_CLIENT_CLASS_NAME = "com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient";
	public static final String BUFFERED_SQS_CLIENT_BEAN_NAME =
			AmazonWebserviceClientConfigurationUtils.getBeanName(AMAZON_BUFFER_CLIENT_CLASS_NAME);

	private AmazonSqsClientBeanConfigurationUtils() {
		// Avoid instantiation
	}

	/**
	 * Registers an {@link com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient} client instance under the default bean name {@link
	 * #BUFFERED_SQS_CLIENT_BEAN_NAME} if not already registered. Creates a {@link com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient} to improve
	 * performance especially while listening to messages from a queue.
	 *
	 * @param registry
	 * 		- the bean definition registry to which the bean should be registered. This registry will be checked if there is
	 * 		already a bean definition.
	 * @param taskExecutor
	 * 		- the task executor bean name used to create the client, might be null if no external task executor is used.
	 * @param regionProvider
	 * 		- regionProvider if a custom is to be configured
	 * @param region
	 * 		- region if the region itself is configured
	 * @return the {@link org.springframework.beans.factory.config.BeanDefinitionHolder} containing the definition along with the registered bean name
	 */
	public static BeanDefinitionHolder registerAmazonSqsClient(
			BeanDefinitionRegistry registry, String taskExecutor, String regionProvider, String region) {

		if (!registry.containsBeanDefinition(BUFFERED_SQS_CLIENT_BEAN_NAME)) {
			BeanDefinitionHolder sqsClient = AmazonWebserviceClientConfigurationUtils.
					registerAmazonWebserviceClient(registry, "com.amazonaws.services.sqs.AmazonSQSAsyncClient", regionProvider, region);

			if (StringUtils.hasText(taskExecutor)) {
				BeanDefinitionBuilder executorBuilder = BeanDefinitionBuilder.genericBeanDefinition(SuppressingExecutorServiceAdapter.class);
				executorBuilder.addConstructorArgReference(taskExecutor);
				sqsClient.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(executorBuilder.getBeanDefinition());
			}

			BeanDefinitionBuilder bufferedClientBuilder = BeanDefinitionBuilder.rootBeanDefinition(AMAZON_BUFFER_CLIENT_CLASS_NAME);
			bufferedClientBuilder.addConstructorArgReference(sqsClient.getBeanName());

			registry.registerBeanDefinition(BUFFERED_SQS_CLIENT_BEAN_NAME, bufferedClientBuilder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(BUFFERED_SQS_CLIENT_BEAN_NAME), BUFFERED_SQS_CLIENT_BEAN_NAME);
	}

}
