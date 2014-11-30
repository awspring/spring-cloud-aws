/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContextRegionProviderConfigurationTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void regionProvider_withConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithStaticRegionProvider.class);

		//Act
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		//Assert
		assertNotNull(staticRegionProvider);
		assertEquals(Region.getRegion(Regions.EU_WEST_1), staticRegionProvider.getRegion());
	}

	@Test
	public void regionProvider_withAutoDetectedRegion_dynamicRegionProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithDynamicRegionProvider.class);

		//Act
		Ec2MetadataRegionProvider staticRegionProvider = this.context.getBean(Ec2MetadataRegionProvider.class);

		//Assert
		assertNotNull(staticRegionProvider);
	}

	@Test
	public void regionProvider_withExpressionConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.<String, Object>singletonMap("region", "EU_WEST_1")));
		this.context.register(ApplicationConfigurationWithExpressionRegion.class);

		// Act
		this.context.refresh();
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		//Assert
		assertNotNull(staticRegionProvider);
		assertEquals(Region.getRegion(Regions.EU_WEST_1), staticRegionProvider.getRegion());
	}

	@Test
	public void regionProvider_withPlaceHolderConfiguredRegion_staticRegionProviderConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.getEnvironment().getPropertySources().addLast(
				new MapPropertySource("test", Collections.<String, Object>singletonMap("region", "EU_WEST_1")));
		this.context.register(ApplicationConfigurationWithPlaceHolderRegion.class);

		// Act
		this.context.refresh();
		StaticRegionProvider staticRegionProvider = this.context.getBean(StaticRegionProvider.class);

		//Assert
		assertNotNull(staticRegionProvider);
		assertEquals(Region.getRegion(Regions.EU_WEST_1), staticRegionProvider.getRegion());
	}

	@Test
	public void regionProvider_withNoRegionAndNoAutoDetection_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanCreationException.class);
		this.expectedException.expectMessage("Region must be manually configured or autoDetect enabled");

		this.context = new AnnotationConfigApplicationContext();

		this.context.register(ApplicationConfigurationWithNoRegion.class);

		// Act
		this.context.refresh();

		//Assert
	}

	@Test
	public void regionProvider_withRegionAndAutoDetection_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanCreationException.class);
		this.expectedException.expectMessage("No region must be configured if autoDetect is defined as true");

		this.context = new AnnotationConfigApplicationContext();

		this.context.register(ApplicationConfigurationWithAutoDetectionAndRegion.class);

		// Act
		this.context.refresh();

		//Assert
	}

	@Test
	public void regionProvider_withConfiguredWrongRegion_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanCreationException.class);
		this.expectedException.expectMessage("No enum constant");

		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithWrongRegion.class);

		//Act

		//Assert
	}

	@Configuration
	@EnableRegionProvider(region = "EU_WEST_1")
	static class ApplicationConfigurationWithStaticRegionProvider {

	}

	@Configuration
	@EnableRegionProvider(autoDetect = true)
	static class ApplicationConfigurationWithDynamicRegionProvider {

	}

	@Configuration
	@EnableRegionProvider(region = "#{environment.region}")
	static class ApplicationConfigurationWithExpressionRegion {

	}

	@Configuration
	@EnableRegionProvider(region = "${region}")
	static class ApplicationConfigurationWithPlaceHolderRegion {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

	@Configuration
	@EnableRegionProvider
	static class ApplicationConfigurationWithNoRegion {

	}

	@Configuration
	@EnableRegionProvider(autoDetect = true, region = "EU_WEST_1")
	static class ApplicationConfigurationWithAutoDetectionAndRegion {

	}

	@Configuration
	@EnableRegionProvider(region = "EU_WAST_1")
	static class ApplicationConfigurationWithWrongRegion {

	}
}