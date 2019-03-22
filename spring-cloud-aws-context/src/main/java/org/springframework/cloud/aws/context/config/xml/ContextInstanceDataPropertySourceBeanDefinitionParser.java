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

package org.springframework.cloud.aws.context.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils.isRunningOnCloudEnvironment;
import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * @author Agim Emruli
 */
class ContextInstanceDataPropertySourceBeanDefinitionParser
		extends AbstractBeanDefinitionParser {

	// @checkstyle:off
	private static final String USER_TAGS_BEAN_CLASS_NAME = "org.springframework.cloud.aws.core.env.ec2.AmazonEc2InstanceUserTagsFactoryBean";

	// @checkstyle:on

	private static final String EC2_CLIENT_CLASS_NAME = "com.amazonaws.services.ec2.AmazonEC2Client";

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {

		if (StringUtils.hasText(element.getAttribute("user-tags-map"))) {
			BeanDefinitionBuilder userTagsBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(USER_TAGS_BEAN_CLASS_NAME);

			userTagsBuilder.addConstructorArgReference(
					getCustomClientOrDefaultClientBeanName(element, parserContext,
							"amazon-ec2", EC2_CLIENT_CLASS_NAME));

			if (StringUtils.hasText(element.getAttribute("instance-id-provider"))) {
				userTagsBuilder.addConstructorArgReference(
						element.getAttribute("instance-id-provider"));
			}
			BeanDefinitionReaderUtils.registerBeanDefinition(
					new BeanDefinitionHolder(userTagsBuilder.getBeanDefinition(),
							element.getAttribute("user-tags-map")),
					parserContext.getRegistry());
		}

		if (isRunningOnCloudEnvironment()) {
			ContextConfigurationUtils.registerInstanceDataPropertySource(
					parserContext.getRegistry(), element.getAttribute("value-separator"),
					element.getAttribute("attribute-separator"));
		}

		return null;
	}

}
