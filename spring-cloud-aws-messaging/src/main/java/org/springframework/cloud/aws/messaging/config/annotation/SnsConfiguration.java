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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

import static org.springframework.cloud.aws.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

/**
 * @author Alain Sahli
 * @since 1.0
 */
@Configuration
public class SnsConfiguration extends WebMvcConfigurerAdapter {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@ConditionalOnMissingAmazonClient(AmazonSNS.class)
	@Bean
	public AmazonSNS amazonSNS() {
		if (this.awsCredentialsProvider != null) {
			return new AmazonSNSClient(this.awsCredentialsProvider);
		} else {
			return new AmazonSNSClient();
		}
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(getNotificationHandlerMethodArgumentResolver(amazonSNS()));
	}
}
