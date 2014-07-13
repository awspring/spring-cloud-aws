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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Agim Emruli
 */
class NotificationArgumentResolverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private String getAmazonSnsClientBeanName(Element element, ParserContext parserContext) {
		String snsClientBeanName;
		if (StringUtils.hasText(element.getAttribute("amazon-sns"))) {
			snsClientBeanName = element.getAttribute("amazon-sns");
		} else {
			snsClientBeanName = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(parserContext.getRegistry(),
					"com.amazonaws.services.sns.AmazonSNSClient", element.getAttribute("region-provider"), element.getAttribute("region")).getBeanName();
		}
		return snsClientBeanName;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.elasticspring.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverFactoryBean";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgReference(getAmazonSnsClientBeanName(element, parserContext));
	}
}
