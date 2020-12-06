/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.appconfig;

import java.nio.ByteBuffer;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import com.amazonaws.services.appconfig.model.GetConfigurationResult;
import org.apache.commons.codec.Resources;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static com.amazonaws.util.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppConfigPropertySourceLocatorTest {

	private AmazonAppConfig appConfigClient = mock(AmazonAppConfig.class);

	private MockEnvironment env = new MockEnvironment();

	@Test
	void whenLoadYamlAppConfigThenReturnPropertySource() throws Exception {
		ByteBuffer content = ByteBuffer.wrap(toByteArray(Resources.getInputStream("config.yaml")));

		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/x-yaml");
		result.setContent(content);

		when(this.appConfigClient.getConfiguration(any())).thenReturn(result);

		AppConfigPropertySourceLocator locator = new AppConfigPropertySourceLocator(this.appConfigClient, "123456789",
				"my-project", "api-my", "dev", null, false);

		this.env.setActiveProfiles("test");

		PropertySource<?> propertySource = locator.locate(env);

		assertThat(propertySource.getProperty("server.port")).isEqualTo(8081);
	}

	@Test
	void whenLoadJsonAppConfigThenReturnPropertySource() throws Exception {
		ByteBuffer content = ByteBuffer.wrap(toByteArray(Resources.getInputStream("config.json")));

		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/json");
		result.setContent(content);

		when(this.appConfigClient.getConfiguration(any())).thenReturn(result);

		AppConfigPropertySourceLocator locator = new AppConfigPropertySourceLocator(this.appConfigClient, "123456789",
				"my-project", "api-my", "dev", null, false);

		this.env.setActiveProfiles("test");

		PropertySource<?> propertySource = locator.locate(env);

		assertThat(propertySource.getProperty("server.port")).isEqualTo(8089);
	}

	@Test
	void whenLoadAnAppConfigAndThrowErrorThenRethrowErrorWhenFailFastTrue() {
		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/json");

		when(this.appConfigClient.getConfiguration(any())).thenThrow(new RuntimeException("connection error"));

		AppConfigPropertySourceLocator locator = new AppConfigPropertySourceLocator(this.appConfigClient, "123456789",
				"my-project", "api-my", "dev", null, true);

		this.env.setActiveProfiles("test");

		assertThatThrownBy(() -> locator.locate(this.env)).hasMessage("connection error");
	}

	@Test
	void whenLoadAnAppConfigAndThrowErrorThenReturnEmptyWhenFailFastFalse() {
		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/json");

		when(this.appConfigClient.getConfiguration(any())).thenThrow(new RuntimeException("connection error"));

		AppConfigPropertySourceLocator locator = new AppConfigPropertySourceLocator(this.appConfigClient, "123456789",
				"my-project", "api-my", "dev", null, false);

		this.env.setActiveProfiles("test");

		assertThatCode(() -> locator.locate(this.env)).doesNotThrowAnyException();
	}

}
