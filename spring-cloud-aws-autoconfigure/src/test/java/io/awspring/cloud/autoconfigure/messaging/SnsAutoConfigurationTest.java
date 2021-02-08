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

package io.awspring.cloud.autoconfigure.messaging;

import java.net.URI;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;
import io.awspring.cloud.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;
import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.REGION_PROVIDER_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SnsAutoConfiguration}.
 *
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 */
class SnsAutoConfigurationTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SnsAutoConfiguration.class));

	@Test
	void enableSns_withMinimalConfig_shouldConfigureACompositeArgumentResolver() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(MinimalSnsConfiguration.class).run((context) -> {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = context
					.getBean(RequestMappingHandlerAdapter.class);

			// Assert
			assertThat(requestMappingHandlerAdapter.getCustomArgumentResolvers()).hasSize(1);
			HandlerMethodArgumentResolver argumentResolver = requestMappingHandlerAdapter.getCustomArgumentResolvers()
					.get(0);
			assertThat(argumentResolver).isInstanceOf(HandlerMethodArgumentResolverComposite.class);

			HandlerMethodArgumentResolverComposite compositeArgumentResolver;
			compositeArgumentResolver = (HandlerMethodArgumentResolverComposite) argumentResolver;
			assertThat(compositeArgumentResolver.getResolvers()).hasSize(3);
			assertThat(getNotificationStatusHandlerMethodArgumentResolver(compositeArgumentResolver.getResolvers()))
					.hasFieldOrProperty("amazonSns").isNotNull();
		});
	}

	@Test
	void enableSns_withProvidedCredentials_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SnsConfigurationWithCredentials.class).run((context) -> {
			AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

			// Assert
			assertThat(amazonSns).hasFieldOrPropertyWithValue("awsCredentialsProvider",
					SnsConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER);
		});
	}

	@Test
	void disableSns() {
		this.contextRunner.withPropertyValues("cloud.aws.sns.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(AmazonSNS.class);
			assertThat(context).doesNotHaveBean(AmazonSNSClient.class);
		});
	}

	@Test
	void enableSns_withCustomAmazonSnsClient_shouldBeUsedByTheArgumentResolver() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SnsConfigurationWithCustomAmazonClient.class).run((context) -> {
			RequestMappingHandlerAdapter requestMappingHandlerAdapter = context
					.getBean(RequestMappingHandlerAdapter.class);

			// Assert
			HandlerMethodArgumentResolverComposite handlerMethodArgumentResolver;
			handlerMethodArgumentResolver = (HandlerMethodArgumentResolverComposite) requestMappingHandlerAdapter
					.getCustomArgumentResolvers().get(0);
			NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver;
			notificationStatusHandlerMethodArgumentResolver = getNotificationStatusHandlerMethodArgumentResolver(
					handlerMethodArgumentResolver.getResolvers());
			assertThat(notificationStatusHandlerMethodArgumentResolver).hasFieldOrPropertyWithValue("amazonSns",
					SnsConfigurationWithCustomAmazonClient.AMAZON_SNS);
		});
	}

	@Test
	void enableSns_withRegionProvided_shouldBeUsedToCreateClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(SnsConfigurationWithRegionProvider.class).run((context) -> {
			AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

			// Assert
			assertThat(ReflectionTestUtils.getField(amazonSns, "endpoint").toString())
					.isEqualTo("https://" + Region.getRegion(Regions.EU_WEST_1).getServiceEndpoint("sns"));
		});
	}

	@Test
	void enableSnsWithSpecificRegion() {
		this.contextRunner.withPropertyValues("cloud.aws.sns.region:us-east-1").run(context -> {
			AmazonSNSClient client = context.getBean(AmazonSNSClient.class);
			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.US_EAST_1.getName());
		});
	}

	@Test
	void enableSnsWithCustomEndpoint() {
		this.contextRunner.withPropertyValues("cloud.aws.sns.endpoint:http://localhost:8090").run(context -> {
			AmazonSNSClient client = context.getBean(AmazonSNSClient.class);

			Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
			assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

			Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client, "isEndpointOverridden");
			assertThat(isEndpointOverridden).isTrue();
		});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalClientConfiguration.class).run((context) -> {
			AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

			// Assert
			ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(amazonSns,
					"clientConfiguration");
			assertThat(clientConfiguration.getProxyHost()).isEqualTo("global");
		});
	}

	@Test
	void configuration_withSnsClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithSnsClientConfiguration.class).run((context) -> {
			AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

			// Assert
			ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(amazonSns,
					"clientConfiguration");
			assertThat(clientConfiguration.getProxyHost()).isEqualTo("sns");
		});
	}

	@Test
	void configuration_withGlobalAndSnsClientConfigurations_shouldUseSnsConfigurationForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalAndSnsClientConfiguration.class)
				.run((context) -> {
					AmazonSNS amazonSns = context.getBean(AmazonSNS.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils
							.getField(amazonSns, "clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("sns");
				});
	}

	@Test
	void doesNotFailWithoutWebMvcConfigurerOnClasspath() {
		this.contextRunner.withUserConfiguration(NoSpringMvcSnsConfiguration.class)
				.withClassLoader(new FilteredClassLoader(WebMvcConfigurer.class)).run((context) -> {
					assertThat(context).hasSingleBean(AmazonSNS.class);
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

	@Configuration
	protected static class NoSpringMvcSnsConfiguration {

	}

	@EnableWebMvc
	protected static class SnsConfigurationWithCredentials {

		static final AWSCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(AWSCredentialsProvider.class);

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

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalClientConfiguration {

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithSnsClientConfiguration {

		@Bean
		ClientConfiguration snsClientConfiguration() {
			return new ClientConfiguration().withProxyHost("sns");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalAndSnsClientConfiguration {

		@Bean
		ClientConfiguration snsClientConfiguration() {
			return new ClientConfiguration().withProxyHost("sns");
		}

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

}
