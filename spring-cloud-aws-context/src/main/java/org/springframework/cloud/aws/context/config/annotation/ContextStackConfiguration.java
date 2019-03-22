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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.AutoDetectingStackNameProvider;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
public class ContextStackConfiguration implements ImportAware {

	private AnnotationAttributes annotationAttributes;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AWSCredentialsProvider credentialsProvider;

	@Autowired(required = false)
	private AmazonEC2 amazonEc2;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes
				.fromMap(importMetadata.getAnnotationAttributes(
						EnableStackConfiguration.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableStackConfiguration is not present on importing class "
						+ importMetadata.getClassName());
	}

	@Bean
	public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(
			AmazonCloudFormation amazonCloudFormation) {
		if (StringUtils.hasText(this.annotationAttributes.getString("stackName"))) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation,
					new StaticStackNameProvider(
							this.annotationAttributes.getString("stackName")));
		}
		else {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation,
					new AutoDetectingStackNameProvider(amazonCloudFormation,
							this.amazonEc2));
		}
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
	public AmazonWebserviceClientFactoryBean<AmazonCloudFormationClient> amazonCloudFormation() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudFormationClient.class,
				this.credentialsProvider, this.regionProvider);
	}

}
