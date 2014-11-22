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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import com.amazonaws.services.rds.AmazonRDSClient;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
@Configuration
@ConditionalOnClass(AmazonRDSClient.class)
public class AmazonRdsDatabaseRegistrar implements ImportBeanDefinitionRegistrar,EnvironmentAware {

	private Environment environment;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

		BeanDefinitionBuilder datasourceBuilder = getBeanDefinitionBuilderForDataSource(this.environment.getProperty("cloud.aws.rds.readReplicaSupport", Boolean.class,false));

		//Constructor (mandatory) args
		String amazonRdsClientBeanName = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(this, beanDefinitionRegistry, AmazonRDSClient.class.getName(), null, null).getBeanName();
		datasourceBuilder.addConstructorArgReference(amazonRdsClientBeanName);
		datasourceBuilder.addConstructorArgValue(this.environment.getRequiredProperty("cloud.aws.rds.dbInstanceIdentifier"));
		datasourceBuilder.addConstructorArgValue(this.environment.getRequiredProperty("cloud.aws.rds.password"));

		//optional args
		if (StringUtils.hasText(this.environment.getProperty("cloud.aws.rds.username"))) {
			datasourceBuilder.addPropertyValue("username", this.environment.getProperty("cloud.aws.rds.username"));
		}

		String resourceResolverBeanName = GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(beanDefinitionRegistry);
		datasourceBuilder.addPropertyReference("resourceIdResolver", resourceResolverBeanName);

		datasourceBuilder.addPropertyValue("dataSourceFactory", BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class).getBeanDefinition());

		beanDefinitionRegistry.registerBeanDefinition(this.environment.getRequiredProperty("cloud.aws.rds.dbInstanceIdentifier"), datasourceBuilder.getBeanDefinition());
	}

	private BeanDefinitionBuilder getBeanDefinitionBuilderForDataSource(boolean readReplicaEnabled) {
		BeanDefinitionBuilder datasourceBuilder;
		if (readReplicaEnabled) {
			datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class);
		} else {
			datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);
		}
		return datasourceBuilder;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
}