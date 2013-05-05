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

import org.elasticspring.messaging.config.AmazonMessagingConfigurationUtils;
import org.elasticspring.messaging.config.annotation.QueueListenerBeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenQueueListenerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final List<String> MESSAGE_LISTENER_COLLABORATORS = Arrays.asList("task-manager", "amazon-sqs");


	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		HashMap<String, Object> defaultProperties = new HashMap<String, Object>();
		for (int i = 0, x = element.getAttributes().getLength(); i < x; i++) {
			Node attribute = element.getAttributes().item(i);
			if (MESSAGE_LISTENER_COLLABORATORS.contains(attribute.getNodeName())) {
				defaultProperties.put(Conventions.attributeNameToPropertyName(attribute.getNodeName()), new RuntimeBeanReference(attribute.getNodeValue()));
			} else {
				defaultProperties.put(Conventions.attributeNameToPropertyName(attribute.getNodeName()), attribute.getNodeValue());
			}
		}

		if (!defaultProperties.containsKey("amazonSqs")) {
			BeanDefinitionHolder definitionHolder = AmazonMessagingConfigurationUtils.registerAmazonSqsClient(parserContext.getRegistry(), element);
			defaultProperties.put("amazonSqs", new RuntimeBeanReference(definitionHolder.getBeanName()));
		}

		builder.addPropertyValue("messageListenerContainerConfiguration", defaultProperties);
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return QueueListenerBeanDefinitionRegistryPostProcessor.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}
}