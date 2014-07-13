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

import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.messaging.core.QueueMessagingTemplate;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import static org.elasticspring.messaging.config.xml.BufferedSqsClientBeanDefinitionUtils.getCustomAmazonSqsClientOrDecoratedDefaultSqsClientBeanName;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String DEFAULT_DESTINATION_ATTRIBUTE = "default-destination";
	private static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";
	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", QueueMessagingTemplateBeanDefinitionParser.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", QueueMessagingTemplateBeanDefinitionParser.class.getClassLoader());

	@Override
	protected Class<?> getBeanClass(Element element) {
		return QueueMessagingTemplate.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String amazonSqsClientBeanName = getCustomAmazonSqsClientOrDecoratedDefaultSqsClientBeanName(element, parserContext);

		if (StringUtils.hasText(element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE))) {
			builder.addPropertyReference(
					Conventions.attributeNameToPropertyName(MESSAGE_CONVERTER_ATTRIBUTE), element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE));
		} else if (JACKSON_2_PRESENT) {
			registerJacksonMessageConverter(builder);
		}

		if (StringUtils.hasText(element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE))) {
			builder.addPropertyReference(
					Conventions.attributeNameToPropertyName(DEFAULT_DESTINATION_ATTRIBUTE), element.getAttribute(DEFAULT_DESTINATION_ATTRIBUTE));
		}

		builder.addConstructorArgReference(amazonSqsClientBeanName);
		builder.addConstructorArgReference(GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
	}

	private void registerJacksonMessageConverter(BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder jacksonBeanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition("org.springframework.messaging.converter.MappingJackson2MessageConverter");
		jacksonBeanDefinitionBuilder.addPropertyValue("serializedPayloadClass", String.class);
		builder.addPropertyValue(
				Conventions.attributeNameToPropertyName(MESSAGE_CONVERTER_ATTRIBUTE), jacksonBeanDefinitionBuilder.getBeanDefinition());
	}

}
