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

package org.springframework.cloud.aws.autoconfigure.messaging;

import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.REGION_PROVIDER_BEAN_NAME;

/**
 * Tests for {@link SnsAutoConfiguration}.
 *
 * @author Alain Sahli
 * @author Maciej Walkowiak
 */
class SnsAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SnsAutoConfiguration.class));

	@Test
	void enableSns_withMinimalConfig_shouldConfigureACompositeArgumentResolver()
			throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(MinimalSnsConfiguration.class)
				.run((context) -> {
					RequestMappingHandlerAdapter requestMappingHandlerAdapter = context
							.getBean(RequestMappingHandlerAdapter.class);

					// Assert
					assertThat(requestMappingHandlerAdapter.getCustomArgumentResolvers())
							.hasSize(1);
					HandlerMethodArgumentResolver argumentResolver = requestMappingHandlerAdapter
							.getCustomArgumentResolvers().get(0);
					assertThat(argumentResolver)
							.isInstanceOf(HandlerMethodArgumentResolverComposite.class);

					HandlerMethodArgumentResolverComposite compositeArgumentResolver;
					compositeArgumentResolver = (HandlerMethodArgumentResolverComposite) argumentResolver;
					assertThat(compositeArgumentResolver.getResolvers()).hasSize(3);
					assertThat(getNotificationStatusHandlerMethodArgumentResolver(
							compositeArgumentResolver.getResolvers()))
									.hasFieldOrProperty("amazonSns").isNotNull();
				});
	}

	@Test
	void enableSns_withProvidedCredentials_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SnsConfigurationWithCredentials.class)
				.run((context) -> {
					AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

					// Assert
					assertThat(amazonSns).hasFieldOrPropertyWithValue(
							"awsCredentialsProvider",
							SnsConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER);
				});
	}

	@Test
	void enableSns_withCustomAmazonSnsClient_shouldBeUsedByTheArgumentResolver()
			throws Exception {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(SnsConfigurationWithCustomAmazonClient.class)
				.run((context) -> {
					RequestMappingHandlerAdapter requestMappingHandlerAdapter = context
							.getBean(RequestMappingHandlerAdapter.class);

					// Assert
					HandlerMethodArgumentResolverComposite handlerMethodArgumentResolver;
					handlerMethodArgumentResolver = (HandlerMethodArgumentResolverComposite) requestMappingHandlerAdapter
							.getCustomArgumentResolvers().get(0);
					NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver;
					notificationStatusHandlerMethodArgumentResolver = getNotificationStatusHandlerMethodArgumentResolver(
							handlerMethodArgumentResolver.getResolvers());
					assertThat(notificationStatusHandlerMethodArgumentResolver)
							.hasFieldOrPropertyWithValue("amazonSns",
									SnsConfigurationWithCustomAmazonClient.AMAZON_SNS);
				});
	}

	@Test
	void enableSns_withRegionProvided_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SnsConfigurationWithRegionProvider.class)
				.run((context) -> {
					AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

					// Assert
					assertThat(ReflectionTestUtils.getField(amazonSns, "endpoint")
							.toString()).isEqualTo(
									"https://" + Region.getRegion(Regions.EU_WEST_1)
											.getServiceEndpoint("sns"));
				});
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

	}

	@EnableWebMvc
	protected static class SnsConfigurationWithCredentials {

		static final AWSCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(
				AWSCredentialsProvider.class);

		@Bean
		AWSCredentialsProvider awsCredentialsProvider() {
			return AWS_CREDENTIALS_PROVIDER;
		}

	}

	@EnableWebMvc
	protected static class SnsConfigurationWithCustomAmazonClient {

		static final AmazonSNS AMAZON_SNS = mock(AmazonSNS.class);

		@Bean
		AmazonSNS amazonSNS() {
			return AMAZON_SNS;
		}

	}

	@EnableWebMvc
	protected static class SnsConfigurationWithRegionProvider {

		@Bean(name = REGION_PROVIDER_BEAN_NAME)
		RegionProvider regionProvider() {
			return new StaticRegionProvider("eu-west-1");
		}

	}

}
