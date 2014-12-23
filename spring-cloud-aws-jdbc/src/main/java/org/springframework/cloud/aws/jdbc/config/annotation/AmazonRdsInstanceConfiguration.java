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

package org.springframework.cloud.aws.jdbc.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.config.support.ContextAnnotationConfigUtil;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
public class AmazonRdsInstanceConfiguration implements ImportAware, BeanFactoryAware {

	private AnnotationAttributes annotationAttributes;

	@Autowired(required = false)
	private AWSCredentialsProvider credentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	private ConfigurableBeanFactory beanFactory;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableRdsInstance.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableRdsInstance is not present on importing class " + importMetadata.getClassName());
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonRDS.class)
	public AmazonRDS amazonRDS() {
		AmazonRDS amazonRds;
		if (this.credentialsProvider != null) {
			amazonRds = new AmazonRDSClient(this.credentialsProvider);
		} else {
			amazonRds = new AmazonRDSClient();
		}

		if (this.regionProvider != null) {
			amazonRds.setRegion(this.regionProvider.getRegion());
		}
		return amazonRds;
	}

	@Bean
	public FactoryBean<DataSource> dataSource(AmazonRDS amazonRds, ResourceIdResolver resourceIdResolver) {
		AmazonRdsDataSourceFactoryBean dataSourceFactoryBean;
		if (this.annotationAttributes.getBoolean("readReplicaSupport")) {
			dataSourceFactoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(amazonRds,
					ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("dbInstanceIdentifier")),
					ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("password")));
		} else {
			dataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(amazonRds,
					ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("dbInstanceIdentifier")),
					ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("password")));
		}

		if (StringUtils.hasText(this.annotationAttributes.getString("username"))) {
			dataSourceFactoryBean.setUsername(ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("username")));
		}

		if (StringUtils.hasText(this.annotationAttributes.getString("databaseName"))) {
			dataSourceFactoryBean.setDatabaseName(ContextAnnotationConfigUtil.resolveStringValue(this.beanFactory, this.annotationAttributes.getString("databaseName")));
		}

		dataSourceFactoryBean.setResourceIdResolver(resourceIdResolver);

		return dataSourceFactoryBean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}
}