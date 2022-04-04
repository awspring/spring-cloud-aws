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
package io.awspring.cloud.autoconfigure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3OutputStream;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.crossregion.CrossRegionS3Client;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Tests for {@link S3AutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class S3AutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, S3AutoConfiguration.class));

	@Test
	void createsS3ClientBean() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(S3Client.class);
			S3Client s3Client = context.getBean(S3Client.class);
			assertThat(s3Client).isInstanceOf(CrossRegionS3Client.class);

			assertThat(context).hasSingleBean(S3ClientBuilder.class);
			assertThat(context).hasSingleBean(S3Properties.class);
			assertThat(context).hasSingleBean(S3OutputStreamProvider.class);
		});
	}

	@Test
	void s3AutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.enabled:false").run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Test
	void s3AutoConfigurationIsDisabled2() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.endpoint:http://localhost:3333")
			.withUserConfiguration(NoTimeoutClientConfiguration.class)
			.run(context -> {
			S3Client s3Client = context.getBean(S3Client.class);
		});
	}

	@Test
	void autoconfigurationIsNotTriggeredWhenS3ModuleIsNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(S3OutputStreamProvider.class)).run(context -> {
			assertThat(context).doesNotHaveBean(S3Client.class);
			assertThat(context).doesNotHaveBean(S3ClientBuilder.class);
			assertThat(context).doesNotHaveBean(S3Properties.class);
		});
	}

	@Test
	void byDefaultCreatesCrossRegionS3Client() {
		this.contextRunner
				.run(context -> assertThat(context).getBean(S3Client.class).isInstanceOf(CrossRegionS3Client.class));
	}

	@Test
	void s3ClientCanBeOverwritten() {
		this.contextRunner.withUserConfiguration(CustomS3ClientConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(S3Client.class);
			assertThat(context).getBean(S3Client.class).isNotInstanceOf(CrossRegionS3Client.class);
		});
	}

	@Test
	void byDefaultCreatesDiskBufferingS3OutputStreamProvider() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(DiskBufferingS3OutputStreamProvider.class));
	}

	@Test
	void customS3OutputStreamProviderCanBeConfigured() {
		this.contextRunner.withUserConfiguration(CustomS3OutputStreamProviderConfiguration.class)
				.run(context -> assertThat(context).hasSingleBean(CustomS3OutputStreamProvider.class));
	}

	@Test
	void createsStandardClientWhenCrossRegionModuleIsNotInClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CrossRegionS3Client.class)).run(context -> {
			assertThat(context).doesNotHaveBean(CrossRegionS3Client.class);
			assertThat(context).hasSingleBean(S3Client.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ClientConfiguration {

		@Bean
		S3Client customS3Client() {
			return mock(S3Client.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3OutputStreamProviderConfiguration {

		@Bean
		S3OutputStreamProvider customS3OutputStreamProvider() {
			return new CustomS3OutputStreamProvider();
		}

	}

	static class CustomS3OutputStreamProvider implements S3OutputStreamProvider {

		@Override
		public S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException {
			return null;
		}

	}

}
