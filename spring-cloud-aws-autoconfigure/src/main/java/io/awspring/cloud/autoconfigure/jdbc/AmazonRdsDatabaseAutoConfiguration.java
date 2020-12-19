/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.autoconfigure.jdbc.AmazonRdsDatabaseProperties.RdsInstance;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.jdbc.config.annotation.AmazonRdsInstanceConfiguration;
import org.springframework.cloud.aws.jdbc.config.annotation.RdsInstanceConfigurerBeanPostProcessor;
import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 * @author Mete Alpaslan Katırcıoğlu
 */
// @checkstyle:off
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(AmazonRdsDatabaseAutoConfiguration.Registrar.class)
@ConditionalOnClass(name = { "com.amazonaws.services.rds.AmazonRDSClient" })
@ConditionalOnMissingBean(AmazonRdsInstanceConfiguration.class)
@ConditionalOnProperty(name = "cloud.aws.rds.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AmazonRdsDatabaseProperties.class)
public class AmazonRdsDatabaseAutoConfiguration {

	// @checkstyle:on

	@Bean
	public static RdsInstanceConfigurerBeanPostProcessor rdsInstanceConfigurerBeanPostProcessor() {
		return new RdsInstanceConfigurerBeanPostProcessor();
	}

	/**
	 * Registrar for Amazon RDS.
	 */
	public static class Registrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		private ConfigurableEnvironment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			AmazonRdsDatabaseProperties properties = rdsDatabaseProperties();
			String endpoint = properties.getEndpoint() != null ? properties.getEndpoint().toString() : null;
			String amazonRdsClientBeanName = AmazonWebserviceClientConfigurationUtils
					.registerAmazonWebserviceClient(this, registry, "com.amazonaws.services.rds.AmazonRDSClient", null,
							properties.getRegion(), endpoint, "rdsClientConfiguration")
					.getBeanName();
			properties.getInstances().stream().filter(RdsInstance::hasRequiredPropertiesSet)
					.forEach(instance -> registerDatasource(registry, amazonRdsClientBeanName, instance));
		}

		private AmazonRdsDatabaseProperties rdsDatabaseProperties() {
			return Binder.get(this.environment).bindOrCreate(AmazonRdsDatabaseProperties.PREFIX,
					AmazonRdsDatabaseProperties.class);
		}

		private static void registerDatasource(BeanDefinitionRegistry registry, String amazonRdsClientBeanName,
				RdsInstance instance) {
			BeanDefinitionBuilder datasourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					instance.isReadReplicaSupport() ? AmazonRdsReadReplicaAwareDataSourceFactoryBean.class
							: AmazonRdsDataSourceFactoryBean.class);

			// Constructor (mandatory) args
			datasourceBuilder.addConstructorArgReference(amazonRdsClientBeanName);
			Assert.hasText(instance.getDbInstanceIdentifier(), "The dbInstanceIdentifier can't be empty.");
			datasourceBuilder.addConstructorArgValue(instance.getDbInstanceIdentifier());
			Assert.hasText(instance.getPassword(), "The password can't be empty.");
			datasourceBuilder.addConstructorArgValue(instance.getPassword());

			// optional args
			datasourceBuilder.addPropertyValue("username", instance.getUsername());
			datasourceBuilder.addPropertyValue("databaseName", instance.getDatabaseName());

			String resourceResolverBeanName = GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(registry);
			datasourceBuilder.addPropertyReference("resourceIdResolver", resourceResolverBeanName);

			datasourceBuilder.addPropertyValue("dataSourceFactory",
					BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class).getBeanDefinition());

			registry.registerBeanDefinition(instance.getDbInstanceIdentifier(), datasourceBuilder.getBeanDefinition());
		}

		@Override
		public void setEnvironment(Environment environment) {
			Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
					"Amazon RDS auto configuration requires a configurable environment");
			this.environment = (ConfigurableEnvironment) environment;
		}

	}

}
