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

package org.elasticspring.context.config.xml;

import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the {@code <els-context:context-resource-loader />} element.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public class ContextResourceLoaderBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	private static final String RESOURCE_LOADER_CLASS_NAME = "org.elasticspring.core.io.s3.SimpleStorageResourceLoader";
	private static final String RESOURCE_RESOLVER_CLASS_NAME = "org.elasticspring.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver";
	private static final String AMAZON_S3_CLIENT_CLASS_NAME = "com.amazonaws.services.s3.AmazonS3Client";
	private static final String RESOURCE_LOADER_BEAN_POST_PROCESSOR = "org.elasticspring.context.support.io.ResourceLoaderBeanPostProcessor";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String amazonS3ClientBeanName = getAmazonS3ClientBeanName(element, parserContext);

		builder.addConstructorArgReference(amazonS3ClientBeanName);
		builder.addConstructorArgValue(getResourceLoaderBeanDefinition(element, parserContext));

		registerPostProcessor(parserContext);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return RESOURCE_RESOLVER_CLASS_NAME;
	}

	@Override
	protected String getBeanClassName(Element element) {
		return RESOURCE_RESOLVER_CLASS_NAME;
	}

	private static String getAmazonS3ClientBeanName(Element element, ParserContext parserContext) {
		if (StringUtils.hasText(element.getAttribute("amazon-s3"))) {
			return element.getAttribute("amazon-s3");
		} else {
			return AmazonWebserviceClientConfigurationUtils.
					registerAmazonWebserviceClient(parserContext.getRegistry(), AMAZON_S3_CLIENT_CLASS_NAME,
							element.getAttribute("region-provider"), element.getAttribute("region")).getBeanName();
		}
	}

	private static BeanDefinition getResourceLoaderBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RESOURCE_LOADER_CLASS_NAME);
		builder.addConstructorArgReference(getAmazonS3ClientBeanName(element, parserContext));

		if (StringUtils.hasText(element.getAttribute("task-executor"))) {
			builder.addPropertyReference(Conventions.attributeNameToPropertyName("task-executor"), element.getAttribute("task-executor"));
		}

		return builder.getBeanDefinition();
	}

	private static void registerPostProcessor(ParserContext parserContext) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(RESOURCE_LOADER_BEAN_POST_PROCESSOR);
		beanDefinitionBuilder.addConstructorArgReference(RESOURCE_RESOLVER_CLASS_NAME);

		BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinitionBuilder.getBeanDefinition(), parserContext.getRegistry());
	}
}