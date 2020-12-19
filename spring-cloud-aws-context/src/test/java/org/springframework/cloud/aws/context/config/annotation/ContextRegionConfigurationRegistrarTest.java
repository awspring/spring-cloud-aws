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

import java.util.Collections;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import io.awspring.cloud.core.region.Ec2MetadataRegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextRegionConfigurationRegistrarTest {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void regionProvider_withConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithStaticRegionProvider.class);

		// Act
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		// Assert
		assertThat(staticRegionProvider).isNotNull();
		assertThat(staticRegionProvider.getRegion()).isEqualTo(Region.getRegion(Regions.EU_WEST_1));
	}

	@Test
	void regionProvider_withAutoDetectedRegion_dynamicRegionProviderConfigured() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithDynamicRegionProvider.class);

		// Act
		Ec2MetadataRegionProvider staticRegionProvider = this.context.getBean(Ec2MetadataRegionProvider.class);

		// Assert
		assertThat(staticRegionProvider).isNotNull();
	}

	@Test
	void regionProvider_withAutoDetectedRegionAndDefaultChain_defaulAwsChainRegionProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithDynamicRegionProvider.class);

		// Act
		Ec2MetadataRegionProvider staticRegionProvider = this.context.getBean(Ec2MetadataRegionProvider.class);

		// Assert
		assertThat(staticRegionProvider).isNotNull();
	}

	@Test
	void regionProvider_withExpressionConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.singletonMap("region", Regions.EU_WEST_1.getName())));
		this.context.register(ApplicationConfigurationWithExpressionRegion.class);

		// Act
		this.context.refresh();
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		// Assert
		assertThat(staticRegionProvider).isNotNull();
		assertThat(staticRegionProvider.getRegion()).isEqualTo(Region.getRegion(Regions.EU_WEST_1));
	}

	@Test
	void regionProvider_withPlaceHolderConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.singletonMap("region", Regions.EU_WEST_1.getName())));
		this.context.register(ApplicationConfigurationWithPlaceHolderRegion.class);

		// Act
		this.context.refresh();
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		// Assert
		assertThat(staticRegionProvider).isNotNull();
		assertThat(staticRegionProvider.getRegion()).isEqualTo(Region.getRegion(Regions.EU_WEST_1));
	}

	@Test
	void regionProvider_withNoRegionAndNoAutoDetection_reportsError() throws Exception {

		this.context = new AnnotationConfigApplicationContext();

		this.context.register(ApplicationConfigurationWithNoRegion.class);

		// Assert
		assertThatThrownBy(() -> this.context.refresh()).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Region must be manually configured or autoDetect enabled");

	}

	@Test
	void regionProvider_withRegionAndAutoDetection_reportsError() throws Exception {

		this.context = new AnnotationConfigApplicationContext();

		this.context.register(ApplicationConfigurationWithAutoDetectionAndRegion.class);

		// Assert
		assertThatThrownBy(() -> this.context.refresh()).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("No region must be configured if autoDetect is defined as true");
	}

	@Test
	void regionProvider_withConfiguredWrongRegion_reportsError() throws Exception {

		assertThatThrownBy(() -> new AnnotationConfigApplicationContext(ApplicationConfigurationWithWrongRegion.class))
				.isInstanceOf(BeanCreationException.class).hasMessageContaining("not a valid region");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(region = "eu-west-1")
	static class ApplicationConfigurationWithStaticRegionProvider {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(autoDetect = true)
	static class ApplicationConfigurationWithDynamicRegionProvider {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(region = "#{environment.region}")
	static class ApplicationConfigurationWithExpressionRegion {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(region = "${region}")
	static class ApplicationConfigurationWithPlaceHolderRegion {

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion
	static class ApplicationConfigurationWithNoRegion {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(autoDetect = true, region = "eu-west-1")
	static class ApplicationConfigurationWithAutoDetectionAndRegion {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(region = "eu-wast-1")
	static class ApplicationConfigurationWithWrongRegion {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableContextRegion(autoDetect = true, useDefaultAwsRegionChain = true)
	static class ApplicationConfigurationWithAutoDetectionAndDefaultChain {

	}

}
