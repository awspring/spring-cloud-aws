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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
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

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableStackConfiguration.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableStackConfiguration is not present on importing class " + importMetadata.getClassName());
	}

	@Bean
	public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(AmazonCloudFormation amazonCloudFormation) {
		if (StringUtils.hasText(this.annotationAttributes.getString("stackName"))) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation, this.annotationAttributes.getString("stackName"));
		}else{
			return new StackResourceRegistryFactoryBean(amazonCloudFormation);
		}
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
	public AmazonCloudFormation amazonCloudFormation() {

		AmazonCloudFormationClient formationClient;
		if (this.credentialsProvider != null) {
			formationClient = new AmazonCloudFormationClient(this.credentialsProvider);
		} else {
			formationClient = new AmazonCloudFormationClient();
		}

		if (this.regionProvider != null) {
			formationClient.setRegion(this.regionProvider.getRegion());
		}
		return formationClient;
	}
}