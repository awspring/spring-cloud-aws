/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.rds.config.xml;

import org.elasticspring.jdbc.retry.DatabaseInstanceStatusRetryPolicy;
import org.elasticspring.jdbc.retry.RdbmsRetryOperationsInterceptor;
import org.elasticspring.jdbc.retry.SqlRetryPolicy;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 *
 */
public class AmazonRdsRetryInterceptorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String RETRY_OPERATIONS_CLASS_NAME = "org.springframework.retry.support.RetryTemplate";
	private static final String COMPOSITE_RETRY_POLICY_CLASS_NAME = "org.springframework.retry.policy.CompositeRetryPolicy";

	private static final String MAX_NUMBER_OF_RETRIES = "max-number-of-retries";
	private static final String BACK_OFF_POLICY = "back-off-policy";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyValue("retryOperations", buildRetryOperationDefinition(element, parserContext));
	}

	private static BeanDefinition buildRetryOperationDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RETRY_OPERATIONS_CLASS_NAME);
		builder.addPropertyValue("retryPolicy", buildRetryPolicyDefinition(element, parserContext));

		if (StringUtils.hasText(element.getAttribute(BACK_OFF_POLICY))) {
			String backOffPolicyBeanName = element.getAttribute(BACK_OFF_POLICY);
			builder.addPropertyReference(Conventions.attributeNameToPropertyName(BACK_OFF_POLICY), backOffPolicyBeanName);
		}

		return builder.getBeanDefinition();
	}

	private static BeanDefinition buildRetryPolicyDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(COMPOSITE_RETRY_POLICY_CLASS_NAME);

		ManagedList<BeanDefinition> policies = new ManagedList<BeanDefinition>(2);
		policies.add(buildDatabaseInstancePolicy(element, parserContext));
		policies.add(buildSQLRetryPolicy(element));

		builder.addPropertyValue("policies", policies);

		return builder.getBeanDefinition();
	}

	private static BeanDefinition buildDatabaseInstancePolicy(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(DatabaseInstanceStatusRetryPolicy.class);
		BeanDefinitionHolder holder = AmazonRdsClientConfigurationUtils.registerAmazonRdsClient(parserContext.getRegistry(), element);
		beanDefinitionBuilder.addConstructorArgReference(holder.getBeanName());
		beanDefinitionBuilder.addConstructorArgValue(element.getAttribute(AmazonRdsBeanDefinitionParser.DB_INSTANCE_IDENTIFIER));
		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BeanDefinition buildSQLRetryPolicy(Element element) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SqlRetryPolicy.class);
		if (StringUtils.hasText(element.getAttribute(MAX_NUMBER_OF_RETRIES))) {
			beanDefinitionBuilder.addPropertyValue(Conventions.attributeNameToPropertyName(MAX_NUMBER_OF_RETRIES), element.getAttribute(MAX_NUMBER_OF_RETRIES));
		}
		return beanDefinitionBuilder.getBeanDefinition();
	}


	@Override
	protected Class<?> getBeanClass(Element element) {
		return RdbmsRetryOperationsInterceptor.class;
	}
}
