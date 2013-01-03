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

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.rds.AmazonRDSClient;
import org.elasticspring.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 */
public class AmazonRdsBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String DB_INSTANCE_IDENTIFIER = "db-instance-identifier";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";


	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);

		BeanDefinitionBuilder amazonRDS = BeanDefinitionBuilder.rootBeanDefinition(AmazonRDSClient.class);
		amazonRDS.addConstructorArgValue(new ClasspathPropertiesFileCredentialsProvider("access.properties"));

		datasourceBuilder.addConstructorArgValue(amazonRDS.getBeanDefinition());

		datasourceBuilder.addPropertyValue(Conventions.attributeNameToPropertyName(DB_INSTANCE_IDENTIFIER), element.getAttribute(DB_INSTANCE_IDENTIFIER));

		if (StringUtils.hasText(element.getAttribute(USERNAME))) {
			datasourceBuilder.addPropertyValue(USERNAME, element.getAttribute(USERNAME));
		}

		datasourceBuilder.addPropertyValue(PASSWORD, element.getAttribute(PASSWORD));
		datasourceBuilder.addPropertyValue("dataSourceFactory", createDataSourceFactoryBeanDefinition(element));

		return datasourceBuilder.getBeanDefinition();
	}

	private static AbstractBeanDefinition createDataSourceFactoryBeanDefinition(Element element) {
		BeanDefinitionBuilder datasourceFactoryBuilder = BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class);
		Element poolAttributes = DomUtils.getChildElementByTagName(element, "pool-attributes");
		NamedNodeMap attributes = poolAttributes.getAttributes();
		for (int i = 0, x = attributes.getLength(); i < x; i++) {
			Node item = attributes.item(i);
			datasourceFactoryBuilder.addPropertyValue(item.getNodeName(), item.getNodeValue());
		}

		return datasourceFactoryBuilder.getBeanDefinition();
	}
}