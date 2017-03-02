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
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@Configuration
@ConditionalOnMissingAmazonClient(AmazonSQS.class)
public class SqsClientConfiguration {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Lazy
	@Bean(destroyMethod = "shutdown")
	public AmazonSQSAsync amazonSQS() {
		AmazonSQSAsyncClientBuilder amazonSQSAsyncClientBuilder = AmazonSQSAsyncClientBuilder.standard();

		if (this.awsCredentialsProvider != null) {
			amazonSQSAsyncClientBuilder.withCredentials(this.awsCredentialsProvider);
		}

		if (this.regionProvider != null) {
			amazonSQSAsyncClientBuilder.withRegion(this.regionProvider.getRegion().getName());
		}

		return new AmazonSQSBufferedAsyncClient(amazonSQSAsyncClientBuilder.build());
	}

}
