/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.sns;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.sns.core.SnsTemplate;
import io.awspring.cloud.sns.core.TopicArnResolver;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Tests for class {@link io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration}.
 *
 * @author Matej Nedic
 */
class SnsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SnsAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void snsAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(SnsClient.class));
	}

	@Test
	void snsAutoConfigurationIsEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(SnsClient.class);
			assertThat(context).hasSingleBean(SnsTemplate.class);
			assertThat(context).hasBean("snsWebMvcConfigurer");

			SnsClient client = context.getBean(SnsClient.class);
			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT))
					.isEqualTo(URI.create("https://sns.eu-west-1.amazonaws.com"));

		});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.sns.endpoint:http://localhost:8090").run(context -> {
			SnsClient client = context.getBean(SnsClient.class);
			assertThat(context).hasSingleBean(SnsTemplate.class);
			assertThat(context).hasBean("snsWebMvcConfigurer");

			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT)).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(attributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN)).isTrue();
		});
	}

	@Test
	void customTopicArnResolverCanBeConfigured() {
		this.contextRunner.withUserConfiguration(CustomTopicArnResolverConfiguration.class)
				.run(context -> assertThat(context).hasSingleBean(CustomTopicArnResolver.class));
	}

	@Test
	void doesNotConfigureArgumentResolversWhenSpringWebNotOnTheClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(WebMvcConfigurer.class)).run(context -> {
			assertThat(context).hasSingleBean(SnsClient.class);
			assertThat(context).hasSingleBean(SnsTemplate.class);
			assertThat(context).doesNotHaveBean("snsWebMvcConfigurer");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTopicArnResolverConfiguration {

		@Bean
		TopicArnResolver customS3OutputStreamProvider() {
			return new CustomTopicArnResolver();
		}

	}

	static class CustomTopicArnResolver implements TopicArnResolver {

		@Override
		public Arn resolveTopicArn(String topicName) {
			return Arn.builder().build();
		}
	}

}
