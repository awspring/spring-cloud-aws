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
package io.awspring.cloud.appconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

public class AppConfigPropertySourceTest {

	private final AppConfigDataClient appConfigDataClient = mock(AppConfigDataClient.class);

	@Test
	void loadYamlAppConfigFile() throws IOException {
		byte[] yamlFile = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("working-spring-dynamodb.yaml"));
		StartConfigurationSessionResponse startConfigurationSessionResponse = StartConfigurationSessionResponse
				.builder().initialConfigurationToken("20").build();

		when(appConfigDataClient.startConfigurationSession((StartConfigurationSessionRequest) any()))
				.thenReturn(startConfigurationSessionResponse);

		GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
				.configuration(SdkBytes.fromByteArray(yamlFile)).contentType("application/x-yaml").build();
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any())).thenReturn(response);
		RequestContext requestContext = RequestContext.builder().applicationIdentifier("myapp")
				.environmentIdentifier("test").configurationProfileIdentifier("1").context("myapp_1_test").build();

		AppConfigPropertySource appConfigPropertySource = new AppConfigPropertySource(requestContext,
				appConfigDataClient);
		appConfigPropertySource.init();
		Assertions.assertThat(appConfigPropertySource.getProperty("spring.cloud.aws.dynamodb.enabled"))
				.isEqualTo(Boolean.TRUE);
		Assertions.assertThat(appConfigPropertySource.getProperty("spring.cloud.aws.dynamodb.endpoint"))
				.isEqualTo("localhost://123123");
	}

	@Test
	void loadJsonAppConfigFile() throws IOException {
		byte[] yamlFile = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("working-json-file.json"));
		StartConfigurationSessionResponse startConfigurationSessionResponse = StartConfigurationSessionResponse
				.builder().initialConfigurationToken("20").build();

		when(appConfigDataClient.startConfigurationSession((StartConfigurationSessionRequest) any()))
				.thenReturn(startConfigurationSessionResponse);

		GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
				.configuration(SdkBytes.fromByteArray(yamlFile)).contentType("application/json").build();
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any())).thenReturn(response);
		RequestContext requestContext = RequestContext.builder().applicationIdentifier("myapp")
				.environmentIdentifier("test").configurationProfileIdentifier("1").context("myapp_1_test").build();

		AppConfigPropertySource appConfigPropertySource = new AppConfigPropertySource(requestContext,
				appConfigDataClient);
		appConfigPropertySource.init();
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.dynamodb.enabled"))
				.isEqualTo(Boolean.TRUE);
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.dynamodb.region")).isEqualTo("eu-west-2");
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.s3.enabled")).isEqualTo(Boolean.FALSE);
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.s3.region")).isEqualTo("eu-west-2");
		Assertions.assertThat(appConfigPropertySource.getProperty("server.port")).isEqualTo(8089);
	}

	@Test
	void loadTextPropertiesAppConfigFile() throws IOException {
		byte[] yamlFile = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("working-properties-file.properties"));
		StartConfigurationSessionResponse startConfigurationSessionResponse = StartConfigurationSessionResponse
				.builder().initialConfigurationToken("20").build();

		when(appConfigDataClient.startConfigurationSession((StartConfigurationSessionRequest) any()))
				.thenReturn(startConfigurationSessionResponse);

		GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
				.configuration(SdkBytes.fromByteArray(yamlFile)).contentType("text/plain").build();
		when(appConfigDataClient.getLatestConfiguration((GetLatestConfigurationRequest) any())).thenReturn(response);
		RequestContext requestContext = RequestContext.builder().applicationIdentifier("myapp")
				.environmentIdentifier("test").configurationProfileIdentifier("1").context("myapp_1_test").build();

		AppConfigPropertySource appConfigPropertySource = new AppConfigPropertySource(requestContext,
				appConfigDataClient);
		appConfigPropertySource.init();
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.sqs.enabled")).isEqualTo("false");
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.dynamodb.region")).isEqualTo("eu-west-2");
		Assertions.assertThat(appConfigPropertySource.getProperty("cloud.aws.s3.enabled")).isEqualTo("false");
	}
}
