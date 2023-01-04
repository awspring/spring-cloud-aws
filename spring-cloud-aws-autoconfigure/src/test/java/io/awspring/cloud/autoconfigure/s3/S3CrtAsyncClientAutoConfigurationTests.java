/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import java.net.URI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Tests for {@link S3CrtAsyncClientAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class S3CrtAsyncClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3CrtAsyncClientAutoConfiguration.class));

	@Nested
	class S3CrtAsyncClientEndpointConfigurationTests {
		@Test
		void withCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.s3.endpoint:http://localhost:8090").run(context -> {
				S3AsyncClient client = context.getBean(S3AsyncClient.class);
				ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
				assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(configuredClient.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090").run(context -> {
				S3AsyncClient client = context.getBean(S3AsyncClient.class);
				ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
				assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
				assertThat(configuredClient.isEndpointOverridden()).isTrue();
			});
		}

		@Test
		void withCustomGlobalEndpointAndS3Endpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:8090",
					"spring.cloud.aws.s3.endpoint:http://localhost:9999").run(context -> {
						S3AsyncClient client = context.getBean(S3AsyncClient.class);
						ConfiguredAwsClient configuredClient = new ConfiguredAwsClient(client);
						assertThat(configuredClient.getEndpoint()).isEqualTo(URI.create("http://localhost:9999"));
						assertThat(configuredClient.isEndpointOverridden()).isTrue();
					});
		}
	}

}
