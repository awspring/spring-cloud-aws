/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.v3.messaging.annotation;

import java.util.List;

import io.awspring.cloud.v3.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import io.awspring.cloud.v3.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static io.awspring.cloud.v3.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
class SnsConfigurationTest {

	private AnnotationConfigWebApplicationContext webApplicationContext;

	@BeforeEach
	void setUp() throws Exception {
		this.webApplicationContext = new AnnotationConfigWebApplicationContext();
		this.webApplicationContext.setServletContext(new MockServletContext());
	}

	@Test
	void enableSns_withMinimalConfig_shouldConfigureACompositeArgumentResolver() throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(MinimalSnsConfiguration.class);
		this.webApplicationContext.refresh();
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = this.webApplicationContext
				.getBean(RequestMappingHandlerAdapter.class);

		// Assert
		assertThat(requestMappingHandlerAdapter.getCustomArgumentResolvers()).hasSize(1);
		HandlerMethodArgumentResolver argumentResolver = requestMappingHandlerAdapter.getCustomArgumentResolvers()
				.get(0);
		assertThat(argumentResolver).isInstanceOf(HandlerMethodArgumentResolverComposite.class);

		HandlerMethodArgumentResolverComposite compositeArgumentResolver;
		compositeArgumentResolver = (HandlerMethodArgumentResolverComposite) argumentResolver;
		assertThat(compositeArgumentResolver.getResolvers().size()).isEqualTo(3);
		assertThat(ReflectionTestUtils.getField(
				getNotificationStatusHandlerMethodArgumentResolver(compositeArgumentResolver.getResolvers()),
				"amazonSns")).isNotNull();
	}

	@Test
	void enableSns_withProvidedCredentials_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithCredentials.class);
		this.webApplicationContext.refresh();
		SnsClient amazonSns = this.webApplicationContext.getBean(SnsClient.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(amazonSns, "awsCredentialsProvider"))
				.isEqualTo(SnsConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER);
	}

	@Test
	void enableSns_withCustomAmazonSnsClient_shouldBeUsedByTheArgumentResolver() throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithCustomAmazonClient.class);
		this.webApplicationContext.refresh();
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = this.webApplicationContext
				.getBean(RequestMappingHandlerAdapter.class);

		// Assert
		HandlerMethodArgumentResolverComposite handlerMethodArgumentResolver;
		handlerMethodArgumentResolver = (HandlerMethodArgumentResolverComposite) requestMappingHandlerAdapter
				.getCustomArgumentResolvers().get(0);
		NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver;
		notificationStatusHandlerMethodArgumentResolver = getNotificationStatusHandlerMethodArgumentResolver(
				handlerMethodArgumentResolver.getResolvers());
		assertThat(ReflectionTestUtils.getField(notificationStatusHandlerMethodArgumentResolver, "amazonSns"))
				.isEqualTo(SnsConfigurationWithCustomAmazonClient.AMAZON_SNS);
	}

	@Test
	void enableSns_withRegionProvided_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithRegionProvider.class);
		this.webApplicationContext.refresh();
		SqsClient amazonSns = this.webApplicationContext.getBean(SqsClient.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(amazonSns, "endpoint").toString())
				.isEqualTo(SqsClient.serviceMetadata().endpointFor(Region.EU_WEST_1));
	}

	private NotificationStatusHandlerMethodArgumentResolver getNotificationStatusHandlerMethodArgumentResolver(
			List<HandlerMethodArgumentResolver> resolvers) {
		for (HandlerMethodArgumentResolver resolver : resolvers) {
			if (resolver instanceof NotificationStatusHandlerMethodArgumentResolver) {
				return (NotificationStatusHandlerMethodArgumentResolver) resolver;

			}
		}

		return null;
	}

	@EnableWebMvc
	protected static class MinimalSnsConfiguration {
		static final SnsClient AMAZON_SNS = mock(SnsClient.class);

		@Bean
		SnsClient amazonSNS() {
			return AMAZON_SNS;
		}

		// Set by Auto Configure
		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(final SnsClient amazonSns) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
					resolvers.add(getNotificationHandlerMethodArgumentResolver(amazonSns));
				}
			};
		}
	}

	@EnableWebMvc
	protected static class SnsConfigurationWithCredentials extends MinimalSnsConfiguration {

		static final AwsCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(AwsCredentialsProvider.class);

		@Bean
		AwsCredentialsProvider awsCredentialsProvider() {
			return AWS_CREDENTIALS_PROVIDER;
		}

	}

	@EnableWebMvc
	protected static class SnsConfigurationWithCustomAmazonClient implements WebMvcConfigurer {

		static final SnsClient AMAZON_SNS = mock(SnsClient.class);

		@Bean
		SnsClient amazonSNS() {
			return AMAZON_SNS;
		}

		@Override
		public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
			resolvers.add(NotificationHandlerMethodArgumentResolverConfigurationUtils
				.getNotificationHandlerMethodArgumentResolver(amazonSNS()));
		}
	}

	@EnableWebMvc
	protected static class SnsConfigurationWithRegionProvider extends MinimalSnsConfiguration {

	}

}
