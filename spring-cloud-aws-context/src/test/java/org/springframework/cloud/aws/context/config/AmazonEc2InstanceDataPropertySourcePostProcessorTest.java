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

package org.springframework.cloud.aws.context.config;

import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceDataPropertySourcePostProcessorTest {

	@Test
	public void postProcessBeanFactory_withConfigurableEnvironment_registersPropertySource()
			throws Exception {
		// Arrange
		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		staticApplicationContext.registerSingleton("process",
				AmazonEc2InstanceDataPropertySourcePostProcessor.class);

		// Act
		staticApplicationContext.refresh();

		// Assert
		assertThat(staticApplicationContext.getEnvironment().getPropertySources().get(
				AmazonEc2InstanceDataPropertySourcePostProcessor.INSTANCE_DATA_PROPERTY_SOURCE_NAME))
						.isNotNull();
	}

	@Test
	public void postProcessBeanFactory_withNonConfigurableEnvironment_skipsRegistration()
			throws Exception {
		// Arrange
		ConfigurableListableBeanFactory staticApplicationContext = new DefaultListableBeanFactory();
		AmazonEc2InstanceDataPropertySourcePostProcessor processor = new AmazonEc2InstanceDataPropertySourcePostProcessor();
		Environment environment = mock(Environment.class);
		processor.setEnvironment(environment);

		// Act
		processor.postProcessBeanFactory(staticApplicationContext);

		// Assert
	}

}
