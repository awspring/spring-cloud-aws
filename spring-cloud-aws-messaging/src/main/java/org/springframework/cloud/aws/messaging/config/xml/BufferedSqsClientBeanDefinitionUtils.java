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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils;
import org.springframework.util.StringUtils;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 */
public final class BufferedSqsClientBeanDefinitionUtils {

	/**
	 * SQS client class name.
	 */
	// @checkstyle:off
	public static final String SQS_CLIENT_CLASS_NAME = "com.amazonaws.services.sqs.AmazonSQSAsyncClient";

	// @checkstyle:on

	// @checkstyle:off
	static final String BUFFERED_SQS_CLIENT_CLASS_NAME = "com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient";

	// @checkstyle:on

	private BufferedSqsClientBeanDefinitionUtils() {
		// Avoid instantiation
	}

	static String getCustomAmazonSqsClientOrDecoratedDefaultSqsClientBeanName(
			Element element, ParserContext parserContext) {
		String amazonSqsClientBeanName = XmlWebserviceConfigurationUtils
				.getCustomClientOrDefaultClientBeanName(element, parserContext,
						"amazon-sqs", SQS_CLIENT_CLASS_NAME);
		if (!StringUtils.hasText(element.getAttribute("amazon-sqs"))) {
			BeanDefinition clientBeanDefinition = parserContext.getRegistry()
					.getBeanDefinition(amazonSqsClientBeanName);
			if (!clientBeanDefinition.getBeanClassName()
					.equals(BUFFERED_SQS_CLIENT_CLASS_NAME)) {
				BeanDefinitionBuilder bufferedClientBeanDefinitionBuilder = BeanDefinitionBuilder
						.rootBeanDefinition(BUFFERED_SQS_CLIENT_CLASS_NAME);
				bufferedClientBeanDefinitionBuilder
						.addConstructorArgValue(clientBeanDefinition);
				parserContext.getRegistry().removeBeanDefinition(amazonSqsClientBeanName);
				parserContext.getRegistry().registerBeanDefinition(
						amazonSqsClientBeanName,
						bufferedClientBeanDefinitionBuilder.getBeanDefinition());
			}
		}
		return amazonSqsClientBeanName;
	}

}
