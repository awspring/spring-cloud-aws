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

package org.springframework.cloud.aws.core.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getAmazonWebserviceClientBeanDefinition;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient;

/**
 * @author Agim Emruli
 */
public final class XmlWebserviceConfigurationUtils {

	private static final String REGION_ATTRIBUTE_NAME = "region";

	private static final String REGION_PROVIDER_ATTRIBUTE_NAME = "region-provider";

	private XmlWebserviceConfigurationUtils() {
		// Avoid instantiation
	}

	public static String getCustomClientOrDefaultClientBeanName(Element element,
			ParserContext parserContext, String customClientAttributeName,
			String serviceClassName) {
		if (StringUtils.hasText(element.getAttribute(customClientAttributeName))) {
			return element.getAttribute(customClientAttributeName);
		}
		else {
			return parseAndRegisterDefaultAmazonWebserviceClient(element, parserContext,
					serviceClassName).getBeanName();
		}
	}

	public static AbstractBeanDefinition parseCustomClientElement(Element element,
			ParserContext parserContext, String serviceClassName) {
		Object source = parserContext.extractSource(element);
		try {
			return getAmazonWebserviceClientBeanDefinition(source, serviceClassName,
					element.getAttribute(REGION_PROVIDER_ATTRIBUTE_NAME),
					element.getAttribute(REGION_ATTRIBUTE_NAME),
					parserContext.getRegistry());
		}
		catch (Exception e) {
			parserContext.getReaderContext().error(e.getMessage(), source, e);
			return null;
		}
	}

	private static BeanDefinitionHolder parseAndRegisterDefaultAmazonWebserviceClient(
			Element element, ParserContext parserContext, String serviceClassName) {
		Object source = parserContext.extractSource(element);
		try {
			return registerAmazonWebserviceClient(source, parserContext.getRegistry(),
					serviceClassName,
					element.getAttribute(REGION_PROVIDER_ATTRIBUTE_NAME),
					element.getAttribute(REGION_ATTRIBUTE_NAME));
		}
		catch (Exception e) {
			parserContext.getReaderContext().error(e.getMessage(), source, e);
			return null;
		}
	}

}
