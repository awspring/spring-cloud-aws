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

import java.util.Optional;

import edu.colorado.cires.cmg.s3out.AwsS3ClientMultipartUpload;
import edu.colorado.cires.cmg.s3out.ContentTypeResolver;
import edu.colorado.cires.cmg.s3out.DefaultContentTypeResolver;
import edu.colorado.cires.cmg.s3out.S3ClientMultipartUpload;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.s3.CrossRegionS3Client;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
			assertThat(context).hasSingleBean(S3ClientBuilder.class);
			assertThat(context).hasSingleBean(S3Properties.class);
			assertThat(context).hasSingleBean(S3ClientMultipartUpload.class);
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
	void byDefaultMultipartClientHasDefaultContentTypeResolver() {
		this.contextRunner.run(context -> {
			AwsS3ClientMultipartUpload clientMultipartUpload = context.getBean(AwsS3ClientMultipartUpload.class);
			assertThat(getContentTypeResolver(clientMultipartUpload)).isInstanceOf(DefaultContentTypeResolver.class);
		});
	}

	@Test
	void multipartContentTypeResolverCanBeConfigured() {
		this.contextRunner.withUserConfiguration(CustomContentTypeResolverConfiguration.class).run(context -> {
			AwsS3ClientMultipartUpload clientMultipartUpload = context.getBean(AwsS3ClientMultipartUpload.class);
			assertThat(getContentTypeResolver(clientMultipartUpload)).isInstanceOf(CustomContentTypeResolver.class);
		});
	}

	@Nullable
	private ContentTypeResolver getContentTypeResolver(AwsS3ClientMultipartUpload bean) {
		return (ContentTypeResolver) ReflectionTestUtils.getField(bean, "contentTypeResolver");
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomS3ClientConfiguration {

		@Bean
		S3Client customS3Client() {
			return mock(S3Client.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomContentTypeResolverConfiguration {

		@Bean
		ContentTypeResolver customContentTypeResolver() {
			return new CustomContentTypeResolver();
		}

	}

	static class CustomContentTypeResolver implements ContentTypeResolver {

		@Override
		public Optional<String> resolveContentType(String s) {
			return Optional.empty();
		}

	}

}
