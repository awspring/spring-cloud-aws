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

package org.elasticspring.messaging.config.xml;

import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.messaging.config.AmazonMessagingConfigurationUtils;
import org.elasticspring.messaging.listener.QueueMessageHandler;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
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

		if (StringUtils.hasText(element.getAttribute("amazon-sqs"))) {
			containerBuilder.addPropertyReference(Conventions.attributeNameToPropertyName("amazon-sqs"), element.getAttribute("amazon-sqs"));
		} else {
			BeanDefinitionHolder definitionHolder = AmazonMessagingConfigurationUtils.registerAmazonSqsClient(parserContext.getRegistry(), element, taskExecutor);
			containerBuilder.addPropertyReference(Conventions.attributeNameToPropertyName("amazon-sqs"), definitionHolder.getBeanName());
		}

		containerBuilder.addPropertyReference("resourceIdResolver", GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
		containerBuilder.addPropertyReference("messageHandler", getMessageHandlerBeanName(parserContext));

		String beanName = parserContext.getReaderContext().generateBeanName(containerBuilder.getBeanDefinition());
		parserContext.getRegistry().registerBeanDefinition(beanName, containerBuilder.getBeanDefinition());

		return null;
	}

	private String getMessageHandlerBeanName(ParserContext parserContext) {
		BeanDefinitionBuilder queueMessageHandlerDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueMessageHandler.class);
		String messageHandlerBeanName = parserContext.getReaderContext().generateBeanName(queueMessageHandlerDefinitionBuilder.getBeanDefinition());
		parserContext.getRegistry().registerBeanDefinition(messageHandlerBeanName, queueMessageHandlerDefinitionBuilder.getBeanDefinition());
		return messageHandlerBeanName;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}
}