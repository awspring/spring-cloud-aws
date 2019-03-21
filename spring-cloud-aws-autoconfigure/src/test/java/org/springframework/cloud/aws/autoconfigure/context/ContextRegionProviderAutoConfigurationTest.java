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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 * @author Petromir Dzhunev
 */
public class ContextRegionProviderAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}

	}

	@Test
	public void regionProvider_autoDetectionConfigured_Ec2metaDataRegionProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextRegionProviderAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.region.auto").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(Ec2MetadataRegionProvider.class)).isNotNull();
	}

	@Test
	public void regionProvider_autoDetectionConfigured_emptyStaticRegionConfigured_Ec2metaDataRegionProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextRegionProviderAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.region.auto").applyTo(this.context);
		TestPropertyValues.of("cloud.aws.region.static:").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(Ec2MetadataRegionProvider.class)).isNotNull();
	}

	@Test
	public void regionProvider_staticRegionConfigured_staticRegionProviderWithConfiguredRegionConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextRegionProviderAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.region.static:eu-west-1").applyTo(this.context);

		// Act
		this.context.refresh();
		StaticRegionProvider regionProvider = this.context
				.getBean(StaticRegionProvider.class);

		// Assert
		assertThat(regionProvider.getRegion())
				.isEqualTo(Region.getRegion(Regions.EU_WEST_1));
	}

}
