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
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
@Configuration
public class SnsConfiguration {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@ConditionalOnMissingAmazonClient(AmazonSNS.class)
	@Bean
	public AmazonSNS amazonSNS() {
		AmazonSNSClient amazonSNSClient;
		if (this.awsCredentialsProvider != null) {
			amazonSNSClient = new AmazonSNSClient(this.awsCredentialsProvider);
		} else {
			amazonSNSClient = new AmazonSNSClient();
		}

		if (this.regionProvider != null) {
			amazonSNSClient.setRegion(this.regionProvider.getRegion());
		}

		return amazonSNSClient;
	}
}