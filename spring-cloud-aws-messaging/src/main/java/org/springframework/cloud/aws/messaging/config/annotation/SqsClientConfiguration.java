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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Alain Sahli
 * @author Eddú Meléndez
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingAmazonClient(AmazonSQS.class)
@Deprecated
public class SqsClientConfiguration {

	private final AWSCredentialsProvider awsCredentialsProvider;

	private final RegionProvider regionProvider;

	public SqsClientConfiguration(ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
			ObjectProvider<RegionProvider> regionProvider) {
		this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
		this.regionProvider = regionProvider.getIfAvailable();
	}

	@Lazy
	@Bean(destroyMethod = "shutdown")
	public AmazonSQSBufferedAsyncClient amazonSQS() throws Exception {
		AmazonWebserviceClientFactoryBean<AmazonSQSAsyncClient> clientFactoryBean = new AmazonWebserviceClientFactoryBean<>(
				AmazonSQSAsyncClient.class, this.awsCredentialsProvider, this.regionProvider);
		clientFactoryBean.afterPropertiesSet();
		return new AmazonSQSBufferedAsyncClient(clientFactoryBean.getObject());
	}

}
