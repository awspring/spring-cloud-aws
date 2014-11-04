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

package org.springframework.cloud.aws.context.config.java;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Agim Emruli
 */
@Configuration
public class ContextCredentialsProviderConfiguration {

	@Autowired
	private Environment environment;

	@Autowired
	private ListableBeanFactory beanFactory;

	@Bean(name = CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME)
	@ConditionalOnMissingBean(name = CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME)
	public FactoryBean<AWSCredentialsProvider> defaultCredentialsProvider() throws Exception {
		Collection<AWSCredentialsProvider> credentialsProviders = this.beanFactory.getBeansOfType(AWSCredentialsProvider.class).values();
		return new CredentialsProviderFactoryBean(new ArrayList<>(credentialsProviders));
	}

	@Bean
	@ConditionalOnProperty("cloud.aws.credentials.accessKey")
	public AWSCredentialsProvider staticCredentialsProvider() {
		return new StaticCredentialsProvider(new BasicAWSCredentials(this.environment.getProperty("cloud.aws.credentials.accessKey"),
				this.environment.getProperty("cloud.aws.credentials.secretKey")));
	}

	@Bean
	@ConditionalOnProperty("cloud.aws.credentials.instanceProfile")
	public AWSCredentialsProvider instanceProfile() {
		return new InstanceProfileCredentialsProvider();
	}
}