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

package org.elasticspring.messaging.config.xml;

import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.messaging.core.QueueMessagingTemplate;
import org.elasticspring.messaging.listener.QueueMessageHandler;
import org.elasticspring.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.elasticspring.messaging.support.SuppressingExecutorServiceAdapter;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for the &lt;annotation-driven-queue-listener/&gt;
 * element.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParser extends AbstractBeanDefinitionParser {

	static final String AMAZON_BUFFER_CLIENT_CLASS_NAME = "com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient";
	public static final String BUFFERED_SQS_CLIENT_BEAN_NAME =
			AmazonWebserviceClientConfigurationUtils.getBeanName(AMAZON_BUFFER_CLIENT_CLASS_NAME);

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder containerBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleMessageListenerContainer.class);

		String taskExecutor = null;
		if (StringUtils.hasText(element.getAttribute("task-executor"))) {
			taskExecutor = element.getAttribute("task-executor");
			containerBuilder.addPropertyReference(Conventions.attributeNameToPropertyName("task-executor"), taskExecutor);
		}

		if (StringUtils.hasText(element.getAttribute("max-number-of-messages"))) {
			containerBuilder.addPropertyValue(Conventions.attributeNameToPropertyName("max-number-of-messages"), element.getAttribute("max-number-of-messages"));
		}

		if (StringUtils.hasText(element.getAttribute("visibility-timeout"))) {
			containerBuilder.addPropertyValue(Conventions.attributeNameToPropertyName("visibility-timeout"), element.getAttribute("visibility-timeout"));
		}

		if (StringUtils.hasText(element.getAttribute("wait-time-out"))) {
			containerBuilder.addPropertyValue(Conventions.attributeNameToPropertyName("wait-time-out"), element.getAttribute("wait-time-out"));
		}

		if (StringUtils.hasText(element.getAttribute("auto-startup"))) {
			containerBuilder.addPropertyValue(Conventions.attributeNameToPropertyName("auto-startup"), element.getAttribute("auto-startup"));
		}

		String sqsClientBeanName;
		if (StringUtils.hasText(element.getAttribute("amazon-sqs"))) {
			sqsClientBeanName = element.getAttribute("amazon-sqs");
		} else {
			BeanDefinitionHolder definitionHolder = registerAmazonSqsClient(parserContext.getRegistry(), taskExecutor,
					element.getAttribute("region-provider"), element.getAttribute("region"));
			sqsClientBeanName = definitionHolder.getBeanName();
		}

		containerBuilder.addPropertyReference(Conventions.attributeNameToPropertyName("amazon-sqs"), sqsClientBeanName);

		containerBuilder.addPropertyReference("resourceIdResolver", GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
		containerBuilder.addPropertyReference("messageHandler", getMessageHandlerBeanName(element, parserContext, sqsClientBeanName));

		return containerBuilder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	private static String getMessageHandlerBeanName(Element element, ParserContext parserContext, String sqsClientBeanName) {
		BeanDefinitionBuilder queueMessageHandlerDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueMessageHandler.class);

		queueMessageHandlerDefinitionBuilder.addPropertyValue("defaultReturnValueHandler",
				createSendToHandlerMethodReturnValueHandlerBeanDefinition(element, parserContext, sqsClientBeanName));

		ManagedList<?> argumentResolvers = getArgumentResolvers(element, parserContext);
		if (!argumentResolvers.isEmpty()) {
			queueMessageHandlerDefinitionBuilder.addPropertyValue("customArgumentResolvers", argumentResolvers);
		}

		ManagedList<?> returnValueHandlers = getReturnValueHandlers(element, parserContext);
		if (!returnValueHandlers.isEmpty()) {
			queueMessageHandlerDefinitionBuilder.addPropertyValue("customReturnValueHandlers", returnValueHandlers);
		}

		String messageHandlerBeanName = parserContext.getReaderContext().generateBeanName(queueMessageHandlerDefinitionBuilder.getBeanDefinition());
		parserContext.getRegistry().registerBeanDefinition(messageHandlerBeanName, queueMessageHandlerDefinitionBuilder.getBeanDefinition());

		return messageHandlerBeanName;
	}

	private static AbstractBeanDefinition createSendToHandlerMethodReturnValueHandlerBeanDefinition(Element element, ParserContext parserContext, String sqsClientBeanName) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SendToHandlerMethodReturnValueHandler.class);
		if (StringUtils.hasText(element.getAttribute("send-to-message-template"))) {
			beanDefinitionBuilder.addConstructorArgReference(element.getAttribute("send-to-message-template"));
		} else {
			BeanDefinitionBuilder templateBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueMessagingTemplate.class);
			templateBuilder.addConstructorArgReference(sqsClientBeanName);
			templateBuilder.addConstructorArgReference(GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
			beanDefinitionBuilder.addConstructorArgValue(templateBuilder.getBeanDefinition());
		}

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static ManagedList<BeanDefinitionHolder> getArgumentResolvers(Element element, ParserContext parserContext) {
		Element resolversElement = DomUtils.getChildElementByTagName(element, "argument-resolvers");
		if (resolversElement != null) {
			return extractBeanSubElements(resolversElement, parserContext);
		} else {
			return new ManagedList<BeanDefinitionHolder>(0);
		}
	}

	private static ManagedList<BeanDefinitionHolder> getReturnValueHandlers(Element element, ParserContext parserContext) {
		Element handlersElement = DomUtils.getChildElementByTagName(element, "return-value-handlers");
		if (handlersElement != null) {
			return extractBeanSubElements(handlersElement, parserContext);
		} else {
			return new ManagedList<BeanDefinitionHolder>(0);
		}
	}

	private static ManagedList<BeanDefinitionHolder> extractBeanSubElements(Element parentElement, ParserContext parserContext) {
		ManagedList<BeanDefinitionHolder> list = new ManagedList<BeanDefinitionHolder>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean")) {
			BeanDefinitionHolder beanDef = parserContext.getDelegate().parseBeanDefinitionElement(beanElement);
			beanDef = parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDef);
			list.add(beanDef);
		}
		return list;
	}

	/**
	 * Registers an {@link com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient} client instance under the default bean name {@link
	 * #BUFFERED_SQS_CLIENT_BEAN_NAME} of not already registered. Creates a {@link com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient} to improve
	 * performance especially while listening to to messages from a queue.
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