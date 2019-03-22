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

package org.springframework.cloud.aws.jdbc.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.jdbc.retry.DatabaseInstanceStatusRetryPolicy;
import org.springframework.cloud.aws.jdbc.retry.RdbmsRetryOperationsInterceptor;
import org.springframework.cloud.aws.jdbc.retry.SqlRetryPolicy;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementation for
 * the <code>retry-interceptor</code> element. This parser produces a
 * {@link org.aopalliance.intercept.MethodInterceptor} which can be used by advice to
 * intercept method calls and retry their particular operation.
 *
 * @author Agim Emruli
 * @since 1.0
 */
class AmazonRdsRetryInterceptorBeanDefinitionParser
		extends AbstractSingleBeanDefinitionParser {

	/**
	 * Class name for the RetryTemplate. String because retry support is optional.
	 */
	private static final String RETRY_OPERATIONS_CLASS_NAME = "org.springframework.retry.support.RetryTemplate";

	/**
	 * Class name used for the policy, which is a composition of two policies (database
	 * instance status and SQL error code).
	 */
	// @checkstyle:off
	private static final String COMPOSITE_RETRY_POLICY_CLASS_NAME = "org.springframework.retry.policy.CompositeRetryPolicy";

	// @checkstyle:on

	/**
	 * Attribute name for the number of retries that should be done.
	 */
	private static final String MAX_NUMBER_OF_RETRIES = "max-number-of-retries";

	/**
	 * Attribute name to a custom back off policy.
	 */
	private static final String BACK_OFF_POLICY = "back-off-policy";

	private static final String AMAZON_RDS_CLIENT_CLASS_NAME = "com.amazonaws.services.rds.AmazonRDSClient";

	/**
	 * Builds the RetryOperation {@link BeanDefinition} with its collaborators.
	 * @param element - <code>retry-interceptor Element</code>
	 * @param parserContext - ParserContext used to query the
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return Configured but non registered bean definition
	 */
	private static BeanDefinition buildRetryOperationDefinition(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(RETRY_OPERATIONS_CLASS_NAME);
		builder.addPropertyValue("retryPolicy",
				buildRetryPolicyDefinition(element, parserContext));

		if (StringUtils.hasText(element.getAttribute(BACK_OFF_POLICY))) {
			String backOffPolicyBeanName = element.getAttribute(BACK_OFF_POLICY);
			builder.addPropertyReference(
					Conventions.attributeNameToPropertyName(BACK_OFF_POLICY),
					backOffPolicyBeanName);
		}

		return builder.getBeanDefinition();
	}

	private static BeanDefinition buildRetryPolicyDefinition(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(COMPOSITE_RETRY_POLICY_CLASS_NAME);

		ManagedList<BeanDefinition> policies = new ManagedList<>(2);
		policies.add(buildDatabaseInstancePolicy(element, parserContext));
		policies.add(buildSQLRetryPolicy(element));

		builder.addPropertyValue("policies", policies);

		return builder.getBeanDefinition();
	}

	private static BeanDefinition buildDatabaseInstancePolicy(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(DatabaseInstanceStatusRetryPolicy.class);

		String amazonRdsClientBeanName = getCustomClientOrDefaultClientBeanName(element,
				parserContext, "amazon-rds", AMAZON_RDS_CLIENT_CLASS_NAME);

		beanDefinitionBuilder.addConstructorArgReference(amazonRdsClientBeanName);
		beanDefinitionBuilder.addConstructorArgValue(element.getAttribute(
				AmazonRdsDataSourceBeanDefinitionParser.DB_INSTANCE_IDENTIFIER));

		String resourceIdResolverBeanName = GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(parserContext.getRegistry());
		beanDefinitionBuilder.addPropertyReference("resourceIdResolver",
				resourceIdResolverBeanName);

		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BeanDefinition buildSQLRetryPolicy(Element element) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(SqlRetryPolicy.class);
		if (StringUtils.hasText(element.getAttribute(MAX_NUMBER_OF_RETRIES))) {
			beanDefinitionBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(MAX_NUMBER_OF_RETRIES),
					element.getAttribute(MAX_NUMBER_OF_RETRIES));
		}
		return beanDefinitionBuilder.getBeanDefinition();
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return RdbmsRetryOperationsInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		builder.addPropertyValue("retryOperations",
				buildRetryOperationDefinition(element, parserContext));
	}

}
