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
package io.awspring.cloud.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Tests for {@link S3ProtocolResolver}.
 *
 * @author Maciej Walkowiak
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
// context must be cleaned up before each method to make sure that for each use
// case
// protocol resolver is registered before resource is requested
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class S3ProtocolResolverTests {

	@Autowired
	@Qualifier("configurationLoadedResource")
	private Resource resource;

	@Value("s3://foo/bar.txt")
	private Resource fieldResource;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	void configurationClassAnnotatedResourceResolvesToS3Resource() throws Exception {
		assertThat(((Advised) resource).getTargetSource().getTarget()).isInstanceOf(S3Resource.class);
	}

	@Test
	void valueAnnotatedResourceResolvesToS3Resource() {
		assertThat(fieldResource).isInstanceOf(S3Resource.class);
	}

	@Test
	void resourceLoadedResourceIsS3Resource() {
		assertThat(resourceLoader.getResource("s3://foo/bar.txt")).isInstanceOf(S3Resource.class);
	}

	@Configuration
	@Import(S3ProtocolResolver.class)
	static class Config {

		@Value("s3://foo/bar.txt")
		@Lazy
		private Resource resource;

		@Bean
		public Resource configurationLoadedResource() {
			return resource;
		}

		@Bean
		S3Client s3Client() {
			return mock(S3Client.class);
		}

		@Bean
		S3OutputStreamProvider s3ClientMultipartUpload() {
			return mock(S3OutputStreamProvider.class);
		}

	}

}
