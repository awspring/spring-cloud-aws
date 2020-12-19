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

package io.awspring.cloud.jdbc.config.annotation;

import io.awspring.cloud.context.config.annotation.ContextDefaultConfigurationRegistrar;
import io.awspring.cloud.context.config.xml.GlobalBeanDefinitionUtils;
import io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils;
import io.awspring.cloud.jdbc.datasource.TomcatJdbcDataSourceFactory;
import io.awspring.cloud.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import io.awspring.cloud.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @deprecated use auto-configuration
 */
// @checkstyle:off
@Configuration(proxyBeanMethods = false)
@Import(ContextDefaultConfigurationRegistrar.class)
@Deprecated
public class AmazonRdsInstanceConfiguration {

	// @checkstyle:on

	@Bean
	public static RdsInstanceConfigurerBeanPostProcessor rdsInstanceConfigurerBeanPostProcessor() {
		return new RdsInstanceConfigurerBeanPostProcessor();
	}

	/**
	 * Abstraction for Amazon RDS client registrar.
	 */
	public abstract static class AbstractRegistrar implements ImportBeanDefinitionRegistrar {

		protected void registerDataSource(BeanDefinitionRegistry beanDefinitionRegistry, String amazonRdsClientBeanName,
				String dbInstanceIdentifier, String password, boolean readReplica, String userName,
				String databaseName) {
			BeanDefinitionBuilder datasourceBuilder = getBeanDefinitionBuilderForDataSource(readReplica);

			// Constructor (mandatory) args
			datasourceBuilder.addConstructorArgReference(amazonRdsClientBeanName);
			Assert.hasText(dbInstanceIdentifier, "The dbInstanceIdentifier can't be empty.");
			datasourceBuilder.addConstructorArgValue(dbInstanceIdentifier);
			Assert.hasText(password, "The password can't be empty.");
			datasourceBuilder.addConstructorArgValue(password);

			// optional args
			datasourceBuilder.addPropertyValue("username", userName);
			datasourceBuilder.addPropertyValue("databaseName", databaseName);

			String resourceResolverBeanName = GlobalBeanDefinitionUtils
					.retrieveResourceIdResolverBeanName(beanDefinitionRegistry);
			datasourceBuilder.addPropertyReference("resourceIdResolver", resourceResolverBeanName);

			datasourceBuilder.addPropertyValue("dataSourceFactory",
					BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class).getBeanDefinition());

			beanDefinitionRegistry.registerBeanDefinition(dbInstanceIdentifier, datasourceBuilder.getBeanDefinition());
		}

		private BeanDefinitionBuilder getBeanDefinitionBuilderForDataSource(boolean readReplicaEnabled) {
			BeanDefinitionBuilder datasourceBuilder;
			if (readReplicaEnabled) {
				datasourceBuilder = BeanDefinitionBuilder
						.rootBeanDefinition(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class);
			}
			else {
				datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);
			}
			return datasourceBuilder;
		}

	}

	/**
	 * Amazon RDS client registrar.
	 */
	public static class Registrar extends AbstractRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			AnnotationAttributes annotationAttributes = AnnotationAttributes
					.fromMap(importingClassMetadata.getAnnotationAttributes(EnableRdsInstance.class.getName(), false));
			Assert.notNull(annotationAttributes,
					"@EnableRdsInstance is not present on importing class " + importingClassMetadata.getClassName());
			String amazonRdsClientBeanName = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(
					this, registry, "com.amazonaws.services.rds.AmazonRDSClient", null, null).getBeanName();
			registerDataSource(registry, amazonRdsClientBeanName,
					annotationAttributes.getString("dbInstanceIdentifier"), annotationAttributes.getString("password"),
					annotationAttributes.getBoolean("readReplicaSupport"), annotationAttributes.getString("username"),
					annotationAttributes.getString("databaseName"));

		}

	}

}
