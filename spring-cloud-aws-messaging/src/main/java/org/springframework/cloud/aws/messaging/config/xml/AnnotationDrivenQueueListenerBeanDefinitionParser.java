/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

import static org.springframework.cloud.aws.messaging.config.xml.BufferedSqsClientBeanDefinitionUtils.getCustomAmazonSqsClientOrDecoratedDefaultSqsClientBeanName;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} for the
 * &lt;annotation-driven-queue-listener/&gt; element.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParser
		extends AbstractBeanDefinitionParser {

	private static final String TASK_EXECUTOR_ATTRIBUTE = "task-executor";

	private static final String MAX_NUMBER_OF_MESSAGES_ATTRIBUTE = "max-number-of-messages";

	private static final String VISIBILITY_TIMEOUT_ATTRIBUTE = "visibility-timeout";

	private static final String WAIT_TIME_OUT_ATTRIBUTE = "wait-time-out";

	private static final String AUTO_STARTUP_ATTRIBUTE = "auto-startup";

	private static final String DESTINATION_RESOLVER_ATTRIBUTE = "destination-resolver";

	private static final String BACK_OFF_TIME = "back-off-time";

	private static String getMessageHandlerBeanName(Element element,
			ParserContext parserContext, String sqsClientBeanName) {
		BeanDefinitionBuilder queueMessageHandlerDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(QueueMessageHandler.class);

		if (parserContext.getRegistry().containsBeanDefinition("jacksonObjectMapper")) {
			queueMessageHandlerDefinitionBuilder
					.addConstructorArgReference("jacksonObjectMapper");
		}
		else {
			BeanDefinitionBuilder mapper = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.messaging.converter.MappingJackson2MessageConverter");
			mapper.addPropertyValue("serializedPayloadClass", "java.lang.String");
			mapper.addPropertyValue("strictContentTypeMatch", true);
			queueMessageHandlerDefinitionBuilder
					.addConstructorArgValue(mapper.getBeanDefinition());
		}

		ManagedList<?> argumentResolvers = getArgumentResolvers(element, parserContext);
		if (!argumentResolvers.isEmpty()) {
			queueMessageHandlerDefinitionBuilder
					.addPropertyValue("customArgumentResolvers", argumentResolvers);
		}

		ManagedList<BeanDefinition> returnValueHandlers = getReturnValueHandlers(element,
				parserContext);
		returnValueHandlers.add(createSendToHandlerMethodReturnValueHandlerBeanDefinition(
				element, parserContext, sqsClientBeanName));
		queueMessageHandlerDefinitionBuilder.addPropertyValue("customReturnValueHandlers",
				returnValueHandlers);

		String messageHandlerBeanName = parserContext.getReaderContext().generateBeanName(
				queueMessageHandlerDefinitionBuilder.getBeanDefinition());
		parserContext.getRegistry().registerBeanDefinition(messageHandlerBeanName,
				queueMessageHandlerDefinitionBuilder.getBeanDefinition());

		return messageHandlerBeanName;
	}

	private static AbstractBeanDefinition createSendToHandlerMethodReturnValueHandlerBeanDefinition(
			Element element, ParserContext parserContext, String sqsClientBeanName) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(SendToHandlerMethodReturnValueHandler.class);
		if (StringUtils.hasText(element.getAttribute("send-to-message-template"))) {
			beanDefinitionBuilder.addConstructorArgReference(
					element.getAttribute("send-to-message-template"));
		}
		else {
			// TODO consider creating a utils for setting up the queue messaging template
			// as it also created in QueueMessagingTemplateBeanDefinitionParser
			BeanDefinitionBuilder templateBuilder = BeanDefinitionBuilder
					.rootBeanDefinition(QueueMessagingTemplate.class);
			templateBuilder.addConstructorArgReference(sqsClientBeanName);
			templateBuilder.addConstructorArgReference(GlobalBeanDefinitionUtils
					.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));

			beanDefinitionBuilder
					.addConstructorArgValue(templateBuilder.getBeanDefinition());
		}

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static ManagedList<BeanDefinition> getArgumentResolvers(Element element,
			ParserContext parserContext) {
		Element resolversElement = DomUtils.getChildElementByTagName(element,
				"argument-resolvers");
		if (resolversElement != null) {
			return extractBeanSubElements(resolversElement, parserContext);
		}
		else {
			return new ManagedList<>(0);
		}
	}

	private static ManagedList<BeanDefinition> getReturnValueHandlers(Element element,
			ParserContext parserContext) {
		Element handlersElement = DomUtils.getChildElementByTagName(element,
				"return-value-handlers");
		if (handlersElement != null) {
			return extractBeanSubElements(handlersElement, parserContext);
		}
		else {
			return new ManagedList<>(0);
		}
	}

	private static ManagedList<BeanDefinition> extractBeanSubElements(
			Element parentElement, ParserContext parserContext) {
		ManagedList<BeanDefinition> list = new ManagedList<>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement,
				"bean")) {
			BeanDefinitionHolder beanDef = parserContext.getDelegate()
					.parseBeanDefinitionElement(beanElement);
			beanDef = parserContext.getDelegate()
					.decorateBeanDefinitionIfRequired(beanElement, beanDef);
			list.add(beanDef.getBeanDefinition());
		}
		return list;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder containerBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(SimpleMessageListenerContainer.class);

		if (StringUtils.hasText(element.getAttribute(TASK_EXECUTOR_ATTRIBUTE))) {
			containerBuilder.addPropertyReference(
					Conventions.attributeNameToPropertyName(TASK_EXECUTOR_ATTRIBUTE),
					element.getAttribute(TASK_EXECUTOR_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(MAX_NUMBER_OF_MESSAGES_ATTRIBUTE))) {
			containerBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(
							MAX_NUMBER_OF_MESSAGES_ATTRIBUTE),
					element.getAttribute(MAX_NUMBER_OF_MESSAGES_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(VISIBILITY_TIMEOUT_ATTRIBUTE))) {
			containerBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(VISIBILITY_TIMEOUT_ATTRIBUTE),
					element.getAttribute(VISIBILITY_TIMEOUT_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(WAIT_TIME_OUT_ATTRIBUTE))) {
			containerBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(WAIT_TIME_OUT_ATTRIBUTE),
					element.getAttribute(WAIT_TIME_OUT_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(AUTO_STARTUP_ATTRIBUTE))) {
			containerBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(AUTO_STARTUP_ATTRIBUTE),
					element.getAttribute(AUTO_STARTUP_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(DESTINATION_RESOLVER_ATTRIBUTE))) {
			containerBuilder.addPropertyReference(
					Conventions
							.attributeNameToPropertyName(DESTINATION_RESOLVER_ATTRIBUTE),
					element.getAttribute(DESTINATION_RESOLVER_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(BACK_OFF_TIME))) {
			containerBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(BACK_OFF_TIME),
					element.getAttribute(BACK_OFF_TIME));
		}

		String amazonSqsClientBeanName = getCustomAmazonSqsClientOrDecoratedDefaultSqsClientBeanName(
				element, parserContext);

		containerBuilder.addPropertyReference(
				Conventions.attributeNameToPropertyName("amazon-sqs"),
				amazonSqsClientBeanName);

		containerBuilder.addPropertyReference("resourceIdResolver",
				GlobalBeanDefinitionUtils
						.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
		containerBuilder.addPropertyReference("messageHandler", getMessageHandlerBeanName(
				element, parserContext, amazonSqsClientBeanName));

		return containerBuilder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

}
