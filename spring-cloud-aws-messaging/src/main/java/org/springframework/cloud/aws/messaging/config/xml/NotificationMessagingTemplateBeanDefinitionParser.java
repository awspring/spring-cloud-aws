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
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * @author Alain Sahli
 */
public class NotificationMessagingTemplateBeanDefinitionParser
		extends AbstractSingleBeanDefinitionParser {

	private static final String DEFAULT_DESTINATION_ATTRIBUTE = "default-destination";

	private static final String SNS_CLIENT_CLASS_NAME = "com.amazonaws.services.sns.AmazonSNSClient";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return NotificationMessagingTemplate.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		if (StringUtils.hasText(element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE))) {
			builder.addPropertyValue("defaultDestinationName",
					element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE));
		}

		builder.addConstructorArgReference(getCustomClientOrDefaultClientBeanName(element,
				parserContext, "amazon-sns", SNS_CLIENT_CLASS_NAME));
		builder.addConstructorArgReference(GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
	}

}
