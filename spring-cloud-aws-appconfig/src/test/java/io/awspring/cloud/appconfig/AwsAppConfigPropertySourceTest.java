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

package io.awspring.cloud.appconfig;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import com.amazonaws.services.appconfig.model.GetConfigurationRequest;
import com.amazonaws.services.appconfig.model.GetConfigurationResult;
import org.apache.commons.codec.Resources;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.amazonaws.util.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsAppConfigPropertySourceTest {

	private AmazonAppConfig appConfigClient = mock(AmazonAppConfig.class);

	@Test
	void requestYamlAppConfigExpectSuccessProperties() throws IOException {
		ByteBuffer content = ByteBuffer.wrap(toByteArray(Resources.getInputStream("config.yaml")));

		ArgumentCaptor<GetConfigurationRequest> captor = ArgumentCaptor.forClass(GetConfigurationRequest.class);

		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/x-yaml");
		result.setContent(content);

		when(appConfigClient.getConfiguration(captor.capture())).thenReturn(result);

		AwsAppConfigPropertySource source = new AwsAppConfigPropertySource("my-api", "123456789", "my-project", "dev",
				null, appConfigClient);

		source.init();

		assertThat(source.getProperty("spring.application.name")).isEqualTo("my-app");
		assertThat(source.getProperty("server.port")).isEqualTo(8081);

		assertThat(captor.getValue().getConfiguration()).isEqualTo("my-api");
	}

	@Test
	public void requestJsonAppConfigExpectSuccessProperties() throws Exception {
		ByteBuffer content = ByteBuffer.wrap(toByteArray(Resources.getInputStream("config.json")));

		ArgumentCaptor<GetConfigurationRequest> captor = ArgumentCaptor.forClass(GetConfigurationRequest.class);

		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("application/json");
		result.setContent(content);

		when(appConfigClient.getConfiguration(captor.capture())).thenReturn(result);

		AwsAppConfigPropertySource source = new AwsAppConfigPropertySource("my-api", "123456789", "my-project", "dev",
				null, appConfigClient);

		source.init();

		assertThat(source.getProperty("spring.application.name")).isEqualTo("my-app");
		assertThat(source.getProperty("server.port")).isEqualTo(8089);

		assertThat(captor.getValue().getConfiguration()).isEqualTo("my-api");
	}

	@Test
	public void requestUnsupportedAppConfigContentType() {
		ArgumentCaptor<GetConfigurationRequest> captor = ArgumentCaptor.forClass(GetConfigurationRequest.class);

		GetConfigurationResult result = new GetConfigurationResult();
		result.setConfigurationVersion("1");
		result.setContentType("text/plain");

		when(appConfigClient.getConfiguration(captor.capture())).thenReturn(result);

		AwsAppConfigPropertySource source = new AwsAppConfigPropertySource("my-api", "123456789", "my-project", "dev",
				null, appConfigClient);

		Assertions.assertThatThrownBy(source::init).isInstanceOf(IllegalStateException.class);
	}

}
