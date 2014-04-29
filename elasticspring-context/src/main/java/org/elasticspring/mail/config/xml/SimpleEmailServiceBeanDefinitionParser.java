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

package org.elasticspring.mail.config.xml;

import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Element;

/**
 * @author Agim Emruli
 */
class SimpleEmailServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@SuppressWarnings("StaticNonFinalField")
	public static boolean isJavaMailPresent = ClassUtils.isPresent("javax.mail.Session", SimpleEmailServiceBeanDefinitionParser.class.getClassLoader());

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinitionHolder holder = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(parserContext.getRegistry(),
				"com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient",
				element.getAttribute("region-provider"),
				element.getAttribute("region"));

		builder.addConstructorArgReference(holder.getBeanName());
	}

	@Override
	protected String getBeanClassName(Element element) {
		if (isJavaMailPresent) {
			return "org.elasticspring.mail.simplemail.SimpleEmailServiceJavaMailSender";
		}

		return "org.elasticspring.mail.simplemail.SimpleEmailServiceMailSender";
	}
}
