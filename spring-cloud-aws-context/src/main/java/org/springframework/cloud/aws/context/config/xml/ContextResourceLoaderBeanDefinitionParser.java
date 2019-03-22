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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * Parser for the {@code <aws-context:context-resource-loader />} element.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@SuppressWarnings({ "UnusedDeclaration", "WeakerAccess" })
public class ContextResourceLoaderBeanDefinitionParser
		extends AbstractSimpleBeanDefinitionParser {

	private static final String AMAZON_S3_CLIENT_CLASS_NAME = "com.amazonaws.services.s3.AmazonS3Client";

	// @checkstyle:off
	private static final String RESOURCE_LOADER_BEAN_POST_PROCESSOR = "org.springframework.cloud.aws.context.support.io.SimpleStorageProtocolResolverConfigurer";

	// @checkstyle:on

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		BeanDefinitionBuilder resolverBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(
						"org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver");
		resolverBuilder.addConstructorArgReference(getCustomClientOrDefaultClientBeanName(
				element, parserContext, "amazon-s3", AMAZON_S3_CLIENT_CLASS_NAME));

		if (StringUtils.hasText(element.getAttribute("task-executor"))) {
			resolverBuilder.addPropertyReference(
					Conventions.attributeNameToPropertyName("task-executor"),
					element.getAttribute("task-executor"));
		}

		builder.addConstructorArgValue(resolverBuilder.getBeanDefinition());
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		return BeanDefinitionReaderUtils.generateBeanName(definition,
				parserContext.getRegistry(), false);
	}

	@Override
	protected String getBeanClassName(Element element) {
		return RESOURCE_LOADER_BEAN_POST_PROCESSOR;
	}

}
