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

package org.elasticspring.messaging.config.xml;

import org.elasticspring.messaging.config.annotation.TopicListenerBeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AnnotationDrivenTopicListenerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {


	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		if (StringUtils.hasText(element.getAttribute("amazon-sns"))) {
			builder.addPropertyValue("amazonSnsBeanName", element.getAttribute("amazon-sns"));
		}

		if (StringUtils.hasText(element.getAttribute("amazon-sqs"))) {
			builder.addPropertyValue("amazonSqsBeanName", element.getAttribute("amazon-sqs"));
		}
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return TopicListenerBeanDefinitionRegistryPostProcessor.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}
}