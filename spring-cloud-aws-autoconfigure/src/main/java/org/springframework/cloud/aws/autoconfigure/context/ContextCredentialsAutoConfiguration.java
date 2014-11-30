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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfiguration;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextDefaultConfiguration.class)
public class ContextCredentialsAutoConfiguration {

	@Autowired
	private Environment environment;

	@Bean(name = {AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME, CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME})
	public FactoryBean<AWSCredentialsProvider> defaultCredentialsProvider() throws Exception {
		List<AWSCredentialsProvider> awsCredentialsProviders = new ArrayList<>();
		if (this.environment.containsProperty("cloud.aws.credentials.accessKey")) {
			awsCredentialsProviders.add(new StaticCredentialsProvider(new BasicAWSCredentials(this.environment.getProperty("cloud.aws.credentials.accessKey"),
					this.environment.getProperty("cloud.aws.credentials.secretKey"))));
		}

		if (this.environment.containsProperty("cloud.aws.credentials.instanceProfile")) {
			awsCredentialsProviders.add(new InstanceProfileCredentialsProvider());
		}

		return new CredentialsProviderFactoryBean(awsCredentialsProviders);
	}


}