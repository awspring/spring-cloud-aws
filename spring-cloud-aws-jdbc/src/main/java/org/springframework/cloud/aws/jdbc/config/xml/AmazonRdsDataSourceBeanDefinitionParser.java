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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} parser
 * implementation for the datasource element. Parses the element and constructs a fully
 * configured {@link AmazonRdsDataSourceFactoryBean} bean definition. Also creates a bean
 * definition for the {@link com.amazonaws.services.rds.AmazonRDSClient} if there is not
 * already an existing one this application context.
 *
 * @author Agim Emruli
 * @since 1.0
 */
class AmazonRdsDataSourceBeanDefinitionParser extends AbstractBeanDefinitionParser {

	static final String DB_INSTANCE_IDENTIFIER = "db-instance-identifier";

	private static final String AMAZON_RDS_CLIENT_CLASS_NAME = "com.amazonaws.services.rds.AmazonRDSClient";

	// @checkstyle:off
	private static final String IDENTITY_MANAGEMENT_CLIENT_CLASS_NAME = "com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient";

	// @checkstyle:on

	// @checkstyle:off
	private static final String USER_TAG_FACTORY_BEAN_CLASS_NAME = "org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceUserTagsFactoryBean";

	// @checkstyle:on

	private static final String USERNAME = "username";

	private static final String PASSWORD = "password";

	private static final String DATABASE_NAME = "database-name";

	/**
	 * Creates a {@link org.springframework.cloud.aws.jdbc.datasource.DataSourceFactory}
	 * implementation. Uses the TomcatJdbcDataSourceFactory implementation and passes all
	 * pool attributes from the xml directly to the class (through setting the bean
	 * properties).
	 * @param element - The datasource element which may contain a pool-attributes element
	 * @return - fully configured bean definition for the DataSourceFactory
	 */
	private static AbstractBeanDefinition createDataSourceFactoryBeanDefinition(
			Element element) {
		BeanDefinitionBuilder datasourceFactoryBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(TomcatJdbcDataSourceFactory.class);
		Element poolAttributes = DomUtils.getChildElementByTagName(element,
				"pool-attributes");
		if (poolAttributes != null) {
			NamedNodeMap attributes = poolAttributes.getAttributes();
			for (int i = 0, x = attributes.getLength(); i < x; i++) {
				Node item = attributes.item(i);
				datasourceFactoryBuilder.addPropertyValue(item.getNodeName(),
						item.getNodeValue());
			}
		}

		return datasourceFactoryBuilder.getBeanDefinition();
	}

	private static void registerUserTagsMapIfNecessary(Element element,
			ParserContext parserContext, String rdsClientBeanName) {
		if (!StringUtils.hasText(element.getAttribute("user-tags-map"))) {
			return;
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(USER_TAG_FACTORY_BEAN_CLASS_NAME);
		builder.addConstructorArgReference(rdsClientBeanName);
		builder.addConstructorArgValue(element.getAttribute(DB_INSTANCE_IDENTIFIER));
		builder.addConstructorArgReference(getCustomClientOrDefaultClientBeanName(element,
				parserContext, "amazon-identity-management",
				IDENTITY_MANAGEMENT_CLIENT_CLASS_NAME));

		// Use custom region-provider of data source
		if (StringUtils.hasText(element.getAttribute("region"))) {
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
					.genericBeanDefinition("com.amazonaws.regions.Region");
			beanDefinitionBuilder.setFactoryMethod("getRegion");
			beanDefinitionBuilder.addConstructorArgValue(element.getAttribute("region"));
			builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
		}
		else {
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(MethodInvokingFactoryBean.class);
			if (StringUtils.hasText(element.getAttribute("region-provider"))) {
				beanDefinitionBuilder.addPropertyValue("targetObject",
						new RuntimeBeanReference(
								element.getAttribute("region-provider")));
			}
			else {
				beanDefinitionBuilder.addPropertyValue("targetObject",
						new RuntimeBeanReference(AmazonWebserviceClientConfigurationUtils
								.getRegionProviderBeanName(parserContext.getRegistry())));
			}
			beanDefinitionBuilder.addPropertyValue("targetMethod", "getRegion");
			builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
		}

		String resourceResolverBeanName = GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(parserContext.getRegistry());
		builder.addPropertyReference("resourceIdResolver", resourceResolverBeanName);

		parserContext.getRegistry().registerBeanDefinition(
				element.getAttribute("user-tags-map"), builder.getBeanDefinition());
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder datasourceBuilder = getBeanDefinitionBuilderForDataSource(
				element);

		// Constructor (mandatory) args
		String amazonRdsClientBeanName = getCustomClientOrDefaultClientBeanName(element,
				parserContext, "amazon-rds", AMAZON_RDS_CLIENT_CLASS_NAME);
		datasourceBuilder.addConstructorArgReference(amazonRdsClientBeanName);
		datasourceBuilder
				.addConstructorArgValue(element.getAttribute(DB_INSTANCE_IDENTIFIER));
		datasourceBuilder.addConstructorArgValue(element.getAttribute(PASSWORD));

		// optional args
		if (StringUtils.hasText(element.getAttribute(USERNAME))) {
			datasourceBuilder.addPropertyValue(USERNAME, element.getAttribute(USERNAME));
		}

		if (StringUtils.hasText(element.getAttribute(DATABASE_NAME))) {
			datasourceBuilder.addPropertyValue(
					Conventions.attributeNameToPropertyName(DATABASE_NAME),
					element.getAttribute(DATABASE_NAME));
		}

		datasourceBuilder.addPropertyValue("dataSourceFactory",
				createDataSourceFactoryBeanDefinition(element));

		// Register registry to enable cloud formation support
		String resourceResolverBeanName = GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(parserContext.getRegistry());
		datasourceBuilder.addPropertyReference("resourceIdResolver",
				resourceResolverBeanName);

		registerUserTagsMapIfNecessary(element, parserContext, amazonRdsClientBeanName);

		return datasourceBuilder.getBeanDefinition();
	}

	private BeanDefinitionBuilder getBeanDefinitionBuilderForDataSource(Element element) {
		BeanDefinitionBuilder datasourceBuilder;
		if (Boolean.TRUE.toString()
				.equalsIgnoreCase(element.getAttribute("read-replica-support"))) {
			datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(
					AmazonRdsReadReplicaAwareDataSourceFactoryBean.class);
		}
		else {
			datasourceBuilder = BeanDefinitionBuilder
					.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);
		}
		return datasourceBuilder;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		return element.getAttribute(DB_INSTANCE_IDENTIFIER);
	}

}
