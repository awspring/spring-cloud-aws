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

package org.springframework.cloud.aws.mail.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * @author Agim Emruli
 */
class SimpleEmailServiceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final boolean JAVA_MAIL_PRESENT = ClassUtils.isPresent(
			"javax.mail.Session",
			SimpleEmailServiceBeanDefinitionParser.class.getClassLoader());

	// @checkstyle:off
	private static final String SIMPLE_EMAIL_CLIENT_CLASS_NAME = "com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient";

	// @checkstyle:on

	@Override
	protected String getBeanClassName(Element element) {
		if (JAVA_MAIL_PRESENT) {
			return "org.springframework.cloud.aws.mail.simplemail.SimpleEmailServiceJavaMailSender";
		}

		return "org.springframework.cloud.aws.mail.simplemail.SimpleEmailServiceMailSender";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		builder.addConstructorArgReference(getCustomClientOrDefaultClientBeanName(element,
				parserContext, "amazon-ses", SIMPLE_EMAIL_CLIENT_CLASS_NAME));
	}

}
