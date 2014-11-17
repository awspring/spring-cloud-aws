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
import org.junit.Test;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
public class SnsConfigurationTest {

	@Test
	public void enableSns_withMinimalConfig_shouldConfigureACompositeArgumentResolver() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SnsConfiguration.class);
		HandlerMethodArgumentResolverComposite compositeArgumentResolver = applicationContext.getBean(HandlerMethodArgumentResolverComposite.class);

		// Assert
		assertEquals(3, compositeArgumentResolver.getResolvers().size());
	}

	@Test
	public void enableSns_withProvidedCredentials_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SnsConfigurationWithCredentials.class);
		AmazonSNS amazonSns = applicationContext.getBean(AmazonSNS.class);

		// Assert
		assertEquals(SnsConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER, ReflectionTestUtils.getField(amazonSns, "awsCredentialsProvider"));
	}

	@Test
	public void enableSns_withCustomAmazonSnsClient_shouldBeUsedByTheArgumentResolver() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SnsConfigurationWithCustomAmazonClient.class);
		HandlerMethodArgumentResolverComposite compositeArgumentResolver = applicationContext.getBean(HandlerMethodArgumentResolverComposite.class);

		// Assert
		NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver = getNotificationStatusHandlerMethodArgumentResolver(compositeArgumentResolver.getResolvers());
		assertEquals(SnsConfigurationWithCustomAmazonClient.AMAZON_SNS, ReflectionTestUtils.getField(notificationStatusHandlerMethodArgumentResolver, "amazonSns"));
	}

	private NotificationStatusHandlerMethodArgumentResolver getNotificationStatusHandlerMethodArgumentResolver(List<HandlerMethodArgumentResolver> resolvers) {
		for (HandlerMethodArgumentResolver resolver : resolvers) {
			if (resolver instanceof NotificationStatusHandlerMethodArgumentResolver) {
				return (NotificationStatusHandlerMethodArgumentResolver) resolver;

			}
		}

		return null;
	}

	@EnableSns
	protected static class SnsConfiguration {

	}

	@EnableSns
	protected static class SnsConfigurationWithCredentials {

		public static final AWSCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(AWSCredentialsProvider.class);

		@Bean
		public AWSCredentialsProvider awsCredentialsProvider() {
			return AWS_CREDENTIALS_PROVIDER;
		}

	}

	@EnableSns
	protected static class SnsConfigurationWithCustomAmazonClient {

		public static final AmazonSNS AMAZON_SNS = mock(AmazonSNS.class);

		@Bean
		public AmazonSNS amazonSns() {
			return AMAZON_SNS;
		}

	}

}
