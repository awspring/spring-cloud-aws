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

package org.springframework.cloud.aws.context.support.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.context.config.annotation.EnableContextResourceLoader;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SimpleStorageProtocolResolverConfigurerIntegrationTest {

	@Autowired
	@Qualifier("configurationLoadedResource")
	private Resource resource;

	@Value("s3://foo/bar.txt")
	private Resource fieldResource;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	public void configurationClassAnnotatedResourceResolvesToS3Resource() throws Exception {
		assertThat(((Advised) resource).getTargetSource().getTarget()).isInstanceOf(SimpleStorageResource.class);
	}

	@Test
	public void valueAnnotatedResourceResolvesToS3Resource() {
		assertThat(fieldResource).isInstanceOf(SimpleStorageResource.class);
	}

	@Test
	public void resourceLoadedResourceIsS3Resource() {
		assertThat(resourceLoader.getResource("s3://foo/bar.txt")).isInstanceOf(SimpleStorageResource.class);
	}

	@Configuration
	@EnableContextResourceLoader
	static class Config {

		@Value("s3://foo/bar.txt")
		@Lazy
		private Resource resource;

		@Bean
		public Resource configurationLoadedResource() {
			return resource;
		}

	}

}
