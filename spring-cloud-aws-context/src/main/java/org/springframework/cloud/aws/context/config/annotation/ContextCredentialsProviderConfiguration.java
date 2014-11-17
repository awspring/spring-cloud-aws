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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Agim Emruli
 */
@Configuration
public class ContextCredentialsProviderConfiguration implements ImportAware {

	private AnnotationAttributes annotationAttributes;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableCredentialsProvider.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableCredentialsProvider is not present on importing class " + importMetadata.getClassName());
	}

	@Bean(name = AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME)
	public FactoryBean<AWSCredentialsProvider> credentialsProvider() throws Exception {
		List<AWSCredentialsProvider> awsCredentialsProviders = new ArrayList<>();

		if (StringUtils.hasText(this.annotationAttributes.getString("accessKey"))) {
			awsCredentialsProviders.add(new StaticCredentialsProvider(new BasicAWSCredentials(this.annotationAttributes.getString("accessKey"),
					this.annotationAttributes.getString("secretKey"))));
		}

		if (this.annotationAttributes.getBoolean("instanceProfile")) {
			awsCredentialsProviders.add(new InstanceProfileCredentialsProvider());
		}

		return new CredentialsProviderFactoryBean(awsCredentialsProviders);
	}
}