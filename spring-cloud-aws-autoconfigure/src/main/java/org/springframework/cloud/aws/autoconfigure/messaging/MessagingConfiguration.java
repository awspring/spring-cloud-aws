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

package org.springframework.cloud.aws.autoconfigure.messaging;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alain Sahli
 */
@Configuration
public class MessagingConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

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

	@ConditionalOnMissingBean(SimpleMessageListenerContainer.class)
	@EnableSqs
	@Configuration
	public static class SqsListenerAutoConfiguration {

	}
}