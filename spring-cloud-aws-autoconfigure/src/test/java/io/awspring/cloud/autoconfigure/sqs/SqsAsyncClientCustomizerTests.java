/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.autoconfigure.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.AwsAsyncClientCustomizer;
import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Tests for {@link SqsAsyncClientCustomizer}.
 *
 * @author Maciej Walkowiak
 */
class SqsAsyncClientCustomizerTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SqsAutoConfiguration.class));

	@Test
	void customClientCustomizer() {
		contextRunner.withUserConfiguration(CustomizerConfig.class).run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SqsAsyncClient.class));
			assertThat(client.getApiCallTimeout()).describedAs("sets property from first customizer")
					.isEqualTo(Duration.ofMillis(2001));
			assertThat(client.getApiCallAttemptTimeout()).describedAs("sets property from second customizer")
					.isEqualTo(Duration.ofMillis(2002));
			assertThat(client.getAsyncHttpClient()).describedAs("sets property from common client customizer")
					.isNotNull();
		});
	}

	@Test
	void customClientCustomizerWithOrder() {
		contextRunner.withUserConfiguration(CustomizerConfigWithOrder.class).run(context -> {
			ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(SqsAsyncClient.class));
			assertThat(client.getApiCallTimeout())
					.describedAs("property from the customizer with higher order takes precedence")
					.isEqualTo(Duration.ofMillis(2001));
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfig {

		@Bean
		SqsAsyncClientCustomizer customizer() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			};
		}

		@Bean
		SqsAsyncClientCustomizer customizer2() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallAttemptTimeout(Duration.ofMillis(2002));
				}));
			};
		}

		@Bean
		AwsAsyncClientCustomizer awsAsyncClientCustomizer() {
			return builder -> {
				builder.httpClient(
						NettyNioAsyncHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build());
			};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfigWithOrder {

		@Bean
		@Order(2)
		SqsAsyncClientCustomizer customizer() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2001));
				}));
			};
		}

		@Bean
		@Order(1)
		SqsAsyncClientCustomizer customizer2() {
			return builder -> {
				builder.overrideConfiguration(builder.overrideConfiguration().copy(c -> {
					c.apiCallTimeout(Duration.ofMillis(2000));
				}));
			};
		}
	}

}
