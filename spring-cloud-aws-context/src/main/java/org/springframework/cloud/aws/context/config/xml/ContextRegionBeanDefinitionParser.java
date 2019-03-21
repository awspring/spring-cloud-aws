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
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.replaceDefaultRegionProvider;

/**
 * Context region provider bean definition parser.
 *
 * @author Agim Emruli
 */
public class ContextRegionBeanDefinitionParser extends AbstractBeanDefinitionParser {

	static final String CONTEXT_REGION_PROVIDER_BEAN_NAME = "regionProvider";

	private static boolean isAutoDetect(Element element) {
		return StringUtils.hasText(element.getAttribute("auto-detect")) && Boolean.TRUE
				.toString().equalsIgnoreCase(element.getAttribute("auto-detect"));
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		return CONTEXT_REGION_PROVIDER_BEAN_NAME;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		if (parserContext.getRegistry()
				.containsBeanDefinition(CONTEXT_REGION_PROVIDER_BEAN_NAME)) {
			parserContext.getReaderContext()
					.error("Multiple <context-region/> elements detected. "
							+ "The <context-region/> element is only allowed once per application context",
							element);
		}

		if (isAutoDetect(element) && (StringUtils.hasText(element.getAttribute("region"))
				|| StringUtils.hasText(element.getAttribute("region-provider")))) {
			parserContext.getReaderContext().error(
					"The attribute 'auto-detect' can only be enabled without a region or region-provider specified",
					element);
			return null;
		}

		if (!isAutoDetect(element) && !StringUtils.hasText(element.getAttribute("region"))
				&& !StringUtils.hasText(element.getAttribute("region-provider"))) {
			parserContext.getReaderContext().error(
					"Either auto-detect must be enabled, or a region or region-provider must be specified",
					element);
			return null;
		}

		// Replace the default region provider with this one
		replaceDefaultRegionProvider(parserContext.getRegistry(),
				CONTEXT_REGION_PROVIDER_BEAN_NAME);

		if (StringUtils.hasText(element.getAttribute("region-provider"))) {
			parserContext.getRegistry().registerAlias(
					element.getAttribute("region-provider"),
					CONTEXT_REGION_PROVIDER_BEAN_NAME);
			return null;
		}
		else if (StringUtils.hasText(element.getAttribute("region"))) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.cloud.aws.core.region.StaticRegionProvider");
			builder.addConstructorArgValue(element.getAttribute("region"));
			return builder.getBeanDefinition();
		}
		else if (isAutoDetect(element)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider");
			return builder.getBeanDefinition();
		}
		return null;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

}
