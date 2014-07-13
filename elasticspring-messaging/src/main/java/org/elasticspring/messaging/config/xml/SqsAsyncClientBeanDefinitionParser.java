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
import org.elasticspring.messaging.support.SuppressingExecutorServiceAdapter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Alain Sahli
 */
public class SqsAsyncClientBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		AbstractBeanDefinition sqsAsyncClientDefinition = AmazonWebserviceClientConfigurationUtils.getAmazonWebserviceClientBeanDefinition(
				BufferedSqsClientBeanDefinitionUtils.SQS_CLIENT_CLASS_NAME,
				element.getAttribute("region-provider"), element.getAttribute("region"));
		if (StringUtils.hasText(element.getAttribute("task-executor"))) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SuppressingExecutorServiceAdapter.class);
			builder.addConstructorArgReference(element.getAttribute("task-executor"));
			sqsAsyncClientDefinition.getConstructorArgumentValues().addIndexedArgumentValue(1, builder.getBeanDefinition());
		}
		if (Boolean.parseBoolean(element.getAttribute("buffered"))) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(BufferedSqsClientBeanDefinitionUtils.BUFFERED_SQS_CLIENT_CLASS_NAME);
			builder.addConstructorArgValue(sqsAsyncClientDefinition);
			return builder.getBeanDefinition();
		} else {
			return sqsAsyncClientDefinition;
		}
	}
}
