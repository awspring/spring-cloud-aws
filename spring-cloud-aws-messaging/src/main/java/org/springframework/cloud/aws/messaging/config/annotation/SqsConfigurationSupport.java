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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;

import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SqsConfigurationSupport {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private ResourceIdResolver resourceIdResolver;

	@Autowired(required = false)
	private SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory;

	@Bean
	public SimpleMessageListenerContainer simpleMessageListenerContainer(AmazonSQS amazonSqs) {
		if (this.simpleMessageListenerContainerFactory == null) {
			this.simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory(amazonSqs);
			this.simpleMessageListenerContainerFactory.setResourceIdResolver(this.resourceIdResolver);
		}

		addReturnValueHandlers(this.simpleMessageListenerContainerFactory.getCustomReturnValueHandlers());
		addArgumentResolvers(this.simpleMessageListenerContainerFactory.getCustomArgumentResolvers());

		return this.simpleMessageListenerContainerFactory.createSimpleMessageListenerContainer();
	}

	@Lazy
	@Bean
	@ConditionalOnMissingAmazonClient(AmazonSQS.class)
	public AmazonSQS amazonSqs() {
		AmazonSQSAsyncClient amazonSQSAsyncClient;
		if (this.awsCredentialsProvider != null) {
			amazonSQSAsyncClient = new AmazonSQSAsyncClient(this.awsCredentialsProvider);
		} else {
			amazonSQSAsyncClient = new AmazonSQSAsyncClient();
		}

		if (this.regionProvider != null) {
			amazonSQSAsyncClient.setRegion(this.regionProvider.getRegion());
		}

		return new AmazonSQSBufferedAsyncClient(amazonSQSAsyncClient);
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

}
