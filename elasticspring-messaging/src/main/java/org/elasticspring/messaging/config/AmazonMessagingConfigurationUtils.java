/*
 * Copyright 2013 the original author or authors.
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

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.elasticspring.messaging.support.SuppressingExecutorServiceAdapter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * Configuration utility class to register default Amazon Webservice SDK beans in case if there is no custom one
 * provided in the configuration.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonMessagingConfigurationUtils {

	/**
	 * Default bean name used inside the application context for the Amazon SQS client
	 */
	public static final String SQS_CLIENT_BEAN_NAME = "SQS_CLIENT";

	/**
	 * Default bean name used inside the application context for the Amazon SNS client
	 */
	public static final String SNS_CLIENT_BEAN_NAME = "SNS_CLIENT";

	/**
	 * Registers an {@link com.amazonaws.services.sqs.AmazonSQSAsync} client instance under the default bean name {@link
	 * #SQS_CLIENT_BEAN_NAME} of not already registered. Creates a {@link AmazonSQSBufferedAsyncClient} to improve
	 * performance especially while listening to to messages from a queue.
	 *
	 * @param registry
	 * 		- the bean definition registry to which the bean should be registered. This registry will be checked if there is
	 * 		already a bean definition.
	 * @param source
	 * 		- the source for the bean definition (e.g. the XML element)
	 * @param taskExecutor
	 * 		- the task executor bean name used to create the client, might be null if no external task executor is used.
	 * @return the {@link BeanDefinitionHolder} containing the definition along with the registered bean name
	 */
	public static BeanDefinitionHolder registerAmazonSqsClient(
			BeanDefinitionRegistry registry, Object source, String taskExecutor) {

		if (!registry.containsBeanDefinition(SQS_CLIENT_BEAN_NAME)) {
			BeanDefinitionBuilder clientBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonSQSAsyncClient.class);
			clientBuilder.getRawBeanDefinition().setSource(source);
			clientBuilder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			clientBuilder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

			if (StringUtils.hasText(taskExecutor)) {
				BeanDefinitionBuilder executorBuilder = BeanDefinitionBuilder.genericBeanDefinition(SuppressingExecutorServiceAdapter.class);
				executorBuilder.addConstructorArgReference(taskExecutor);
				clientBuilder.addConstructorArgValue(executorBuilder.getBeanDefinition());
			}

			BeanDefinitionBuilder bufferedClientBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonSQSBufferedAsyncClient.class);
			bufferedClientBuilder.addConstructorArgValue(clientBuilder.getBeanDefinition());

			registry.registerBeanDefinition(SQS_CLIENT_BEAN_NAME, bufferedClientBuilder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(SQS_CLIENT_BEAN_NAME), SQS_CLIENT_BEAN_NAME);
	}

	/**
	 * Registers a {@link com.amazonaws.services.sns.AmazonSNS} instance with the bean name {@link #SNS_CLIENT_BEAN_NAME}
	 * if not already existing.
	 *
	 * @param registry
	 * 		- the bean definition registry to which the bean should be registered. This registry will be checked if there is
	 * 		already a bean definition.
	 * @param source
	 * 		- the source for the bean definition (e.g. the XML element)
	 * @return the {@link BeanDefinitionHolder} containing the definition along with the registered bean name
	 */
	public static BeanDefinitionHolder registerAmazonSnsClient(
			BeanDefinitionRegistry registry, Object source) {

		if (!registry.containsBeanDefinition(SNS_CLIENT_BEAN_NAME)) {
			BeanDefinitionBuilder clientBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonSNSClient.class);
			clientBuilder.getRawBeanDefinition().setSource(source);
			clientBuilder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			clientBuilder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

			registry.registerBeanDefinition(SNS_CLIENT_BEAN_NAME, clientBuilder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(SNS_CLIENT_BEAN_NAME), SNS_CLIENT_BEAN_NAME);
	}
}
