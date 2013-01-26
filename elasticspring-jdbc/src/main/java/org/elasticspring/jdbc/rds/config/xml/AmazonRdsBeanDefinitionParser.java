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

package org.elasticspring.jdbc.rds.config.xml;

import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.elasticspring.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.elasticspring.jdbc.rds.AmazonRdsClientFactoryBean;
import org.elasticspring.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} parser implementation for the data source
 * element. Parses the element and constructs a fully configured {@link AmazonRdsDataSourceFactoryBean} bean
 * definition. Also creates a bean definition for the {@link AmazonRdsClientFactoryBean} if there is not already an
 * existing one this application context.
 *
 * @author Agim Emruli
 * @since 1.0
 */
@SuppressWarnings("UnusedDeclaration")
public class AmazonRdsBeanDefinitionParser extends AbstractBeanDefinitionParser {

	/**
	 * The bean name which will be used to register the AmazonRdsClientFactoryBean, will re-use the bean if there is
	 * already an exiting one (e.g. multiple data source elements in one application context)
	 */
	static final String RDS_CLIENT_BEAN_NAME = "RDS_CLIENT";
	private static final String DB_INSTANCE_IDENTIFIER = "db-instance-identifier";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";


	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);

		//Check if the AmazonRDS client is already available in the registry, or create a new one
		if (!parserContext.getRegistry().containsBeanDefinition(RDS_CLIENT_BEAN_NAME)) {
			BeanDefinitionBuilder amazonRDS = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsClientFactoryBean.class);
			amazonRDS.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
			parserContext.getRegistry().registerBeanDefinition(RDS_CLIENT_BEAN_NAME, amazonRDS.getBeanDefinition());
		}

		//Constructor (mandatory) args
		datasourceBuilder.addConstructorArgReference(RDS_CLIENT_BEAN_NAME);
		datasourceBuilder.addConstructorArgValue(element.getAttribute(DB_INSTANCE_IDENTIFIER));
		datasourceBuilder.addConstructorArgValue(element.getAttribute(PASSWORD));

		//optional args
		if (StringUtils.hasText(element.getAttribute(USERNAME))) {
			datasourceBuilder.addPropertyValue(USERNAME, element.getAttribute(USERNAME));
		}

		datasourceBuilder.addPropertyValue("dataSourceFactory", createDataSourceFactoryBeanDefinition(element));

		return datasourceBuilder.getBeanDefinition();
	}

	/**
	 * Creates a {@link org.elasticspring.jdbc.datasource.DataSourceFactory} implementation. Uses the
	 * TomcatJdbcDataSourceFactory implementation and passes all pool attributes from the xml directly to the class
	 * (through setting the bean properties).
	 *
	 * @param element
	 * 		- The data source element which may contain a pool-attributes element
	 * @return - fully configured bean definition for the DataSourceFactory
	 */
	private static AbstractBeanDefinition createDataSourceFactoryBeanDefinition(Element element) {
		BeanDefinitionBuilder datasourceFactoryBuilder = BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class);
		Element poolAttributes = DomUtils.getChildElementByTagName(element, "pool-attributes");
		if (poolAttributes != null) {
			NamedNodeMap attributes = poolAttributes.getAttributes();
			for (int i = 0, x = attributes.getLength(); i < x; i++) {
				Node item = attributes.item(i);
				datasourceFactoryBuilder.addPropertyValue(item.getNodeName(), item.getNodeValue());
			}
		}

		return datasourceFactoryBuilder.getBeanDefinition();
	}
}