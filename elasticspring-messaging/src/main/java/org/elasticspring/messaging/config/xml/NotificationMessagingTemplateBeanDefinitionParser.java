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
import org.elasticspring.messaging.core.NotificationMessagingTemplate;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Alain Sahli
 */
public class NotificationMessagingTemplateBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String DEFAULT_DESTINATION_ATTRIBUTE = "default-destination";
	private static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return NotificationMessagingTemplate.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String amazonSnsClientBeanName;
		if (StringUtils.hasText(element.getAttribute("amazon-sns"))) {
			amazonSnsClientBeanName = element.getAttribute("amazon-sns");
		} else {
			amazonSnsClientBeanName = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(
					parserContext.getRegistry(), "com.amazonaws.services.sns.AmazonSNSClient", null, null).getBeanName();
		}

		if (StringUtils.hasText(element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE))) {
			builder.addPropertyReference(
					Conventions.attributeNameToPropertyName(MESSAGE_CONVERTER_ATTRIBUTE), element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE));
		}

		if (StringUtils.hasText(element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE))) {
			builder.addPropertyReference(
					Conventions.attributeNameToPropertyName(DEFAULT_DESTINATION_ATTRIBUTE), element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE));
		}

		builder.addConstructorArgReference(amazonSnsClientBeanName);
		builder.addConstructorArgReference(GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
	}

}
