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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.core.task.ShutdownSuppressingExecutorServiceAdapter;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.parseCustomClientElement;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 */
public class SqsAsyncClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		AbstractBeanDefinition sqsAsyncClientDefinition = parseCustomClientElement(
				element, parserContext,
				BufferedSqsClientBeanDefinitionUtils.SQS_CLIENT_CLASS_NAME);
		if (StringUtils.hasText(element.getAttribute("task-executor"))) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					ShutdownSuppressingExecutorServiceAdapter.class);
			builder.addConstructorArgReference(element.getAttribute("task-executor"));
			sqsAsyncClientDefinition.getPropertyValues().addPropertyValue("executor",
					builder.getBeanDefinition());
		}
		if (Boolean.parseBoolean(element.getAttribute("buffered"))) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					BufferedSqsClientBeanDefinitionUtils.BUFFERED_SQS_CLIENT_CLASS_NAME);
			builder.addConstructorArgValue(sqsAsyncClientDefinition);
			return builder.getBeanDefinition();
		}
		else {
			return sqsAsyncClientDefinition;
		}
	}

}
