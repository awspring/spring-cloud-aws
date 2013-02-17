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

package org.elasticspring.context.config.xml;

import org.elasticspring.context.config.AmazonS3FactoryBean;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.elasticspring.context.support.io.ResourceLoaderBeanPostProcessor;
import org.elasticspring.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.elasticspring.core.io.s3.S3ServiceEndpoint;
import org.elasticspring.core.region.StaticRegionProvider;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

class SimpleStorageLoaderBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	private static final String AMAZON_S3_BEAN_NAME = "AMAZON_S3";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		if (!parserContext.getRegistry().containsBeanDefinition(AMAZON_S3_BEAN_NAME)) {
			BeanDefinitionBuilder amazonsS3Builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonS3FactoryBean.class);
			amazonsS3Builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
			addRegionProviderBeanDefinition(element, parserContext, amazonsS3Builder);
			parserContext.getRegistry().registerBeanDefinition(AMAZON_S3_BEAN_NAME, amazonsS3Builder.getBeanDefinition());
		}

		builder.addConstructorArgReference(AMAZON_S3_BEAN_NAME);

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(ResourceLoaderBeanPostProcessor.class);
		beanDefinitionBuilder.addConstructorArgReference(PathMatchingSimpleStorageResourcePatternResolver.class.getName());
		AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
		String beanName = parserContext.getReaderContext().generateBeanName(beanDefinition);
		parserContext.getRegistry().registerBeanDefinition(beanName, beanDefinition);
	}

	private static void addRegionProviderBeanDefinition(Element element, ParserContext parserContext, BeanDefinitionBuilder parent) {
		if (StringUtils.hasText(element.getAttribute("region")) && StringUtils.hasText(element.getAttribute("region-provider-ref"))) {
			parserContext.getReaderContext().error("region and region-provider-ref attribute must not be used together", element);
			return;
		}

		if (StringUtils.hasText(element.getAttribute("region-provider-ref"))) {
			parent.addConstructorArgReference(element.getAttribute("region-provider-ref"));
			return;
		}

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(StaticRegionProvider.class);
		if (StringUtils.hasText(element.getAttribute("region"))) {
			beanDefinitionBuilder.addConstructorArgValue(S3ServiceEndpoint.valueOf(element.getAttribute("region")));
			parent.addConstructorArgValue(beanDefinitionBuilder.getBeanDefinition());
		} else {
			beanDefinitionBuilder.addConstructorArgValue(S3ServiceEndpoint.US_STANDARD);
		}

		parent.addConstructorArgValue(beanDefinitionBuilder.getBeanDefinition());
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return PathMatchingSimpleStorageResourcePatternResolver.class.getName();
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return PathMatchingSimpleStorageResourcePatternResolver.class;
	}
}