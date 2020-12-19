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

package io.awspring.cloud.context.config.annotation;

import io.awspring.cloud.core.io.s3.SimpleStorageProtocolResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ContextResourceLoaderConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void regionProvider_withConfiguredRegion_staticRegionProviderConfigured() {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithResourceLoader.class);

		// Act
		ApplicationBean bean = this.context.getBean(ApplicationBean.class);

		// Assert
		assertThat(bean.getResourceLoader()).isNotNull();
		assertThat(DefaultResourceLoader.class.isInstance(bean.getResourceLoader())).isTrue();

		DefaultResourceLoader defaultResourceLoader = (DefaultResourceLoader) bean.getResourceLoader();
		assertThat(defaultResourceLoader.getProtocolResolvers().iterator().next())
				.isInstanceOf(SimpleStorageProtocolResolver.class);
	}

	@EnableContextResourceLoader
	static class ApplicationConfigurationWithResourceLoader {

		@Bean
		ApplicationBean appBean() {
			return new ApplicationBean();
		}

	}

	static class ApplicationBean {

		@Autowired
		private ResourceLoader resourceLoader;

		private ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

	}

}
