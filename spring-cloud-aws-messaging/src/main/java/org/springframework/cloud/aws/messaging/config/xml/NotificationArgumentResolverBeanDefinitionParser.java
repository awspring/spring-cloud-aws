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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * @author Agim Emruli
 */
class NotificationArgumentResolverBeanDefinitionParser
		extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.cloud.aws.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverFactoryBean";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		builder.addConstructorArgReference(
				getCustomClientOrDefaultClientBeanName(element, parserContext,
						"amazon-sns", "com.amazonaws.services.sns.AmazonSNSClient"));
	}

}
