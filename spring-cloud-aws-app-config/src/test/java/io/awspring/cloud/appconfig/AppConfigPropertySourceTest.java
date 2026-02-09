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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationResponse;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionResponse;

/**
 * Tests for {@link AppConfigPropertySource}.
 *
 * @author Matej Nedic
 */
class AppConfigPropertySourceTest {

	private static final RequestContext DEFAULT_CONTEXT = new RequestContext("profile1", "env1", "app1",
			"app1#profile1#env1");

	private static final String RESOURCE_PATH = "io/awspring/cloud/appconfig/";

	private final AppConfigDataClient client = mock(AppConfigDataClient.class);

	private static String loadResource(String filename) {
		try {
			ClassPathResource resource = new ClassPathResource(RESOURCE_PATH + filename);
			return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load test resource: " + filename, e);
		}
	}

	@BeforeEach
	void setupDefaultSessionMock() {
		when(client.startConfigurationSession(any(StartConfigurationSessionRequest.class)))
			.thenReturn(StartConfigurationSessionResponse.builder().initialConfigurationToken("initial-token").build());
	}

	@Test
	void shouldParsePropertiesContentType() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		when(client.startConfigurationSession(any(StartConfigurationSessionRequest.class)))
			.thenAnswer(invocation -> {
				StartConfigurationSessionRequest request = invocation.getArgument(0);
				assertThat(request.applicationIdentifier()).isEqualTo("app1");
				assertThat(request.environmentIdentifier()).isEqualTo("env1");
				assertThat(request.configurationProfileIdentifier()).isEqualTo("profile1");
				return StartConfigurationSessionResponse.builder().initialConfigurationToken("initial-token").build();
			});

		GetLatestConfigurationResponse response = buildResponse(loadResource("test-config.properties"), "text/plain");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class)))
			.thenAnswer(invocation -> {
				GetLatestConfigurationRequest request = invocation.getArgument(0);
				assertThat(request.configurationToken()).isEqualTo("initial-token");
				return response;
			});

		propertySource.init();

		assertThat(propertySource.getName()).isEqualTo("aws-appconfig:" + DEFAULT_CONTEXT);
		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("key1", "key2");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void shouldParseYamlContentType() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse(
				loadResource("test-config.yaml"), "application/x-yaml");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		assertThat(propertySource.getName()).isEqualTo("aws-appconfig:" + DEFAULT_CONTEXT);
		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("key1", "key2", "nested.key3");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
		assertThat(propertySource.getProperty("nested.key3")).isEqualTo("value3");
	}

	@Test
	void shouldParseAlternativeYamlContentType() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse(loadResource("test-config.yaml"), "text/yaml");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("key1", "key2", "nested.key3");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
	}

	@Test
	void shouldParseJsonContentType() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse(
				loadResource("test-config.json"), "application/json");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactlyInAnyOrder("key1", "key2");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void throwsExceptionForUnsupportedContentType() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse("some content", "application/xml");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		assertThatThrownBy(propertySource::init)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Cannot parse unknown content type: application/xml");
	}

	@Test
	void shouldInitializeSessionTokenOnFirstInit() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse("key1=value1", "text/plain");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		verify(client, times(1)).startConfigurationSession(any(StartConfigurationSessionRequest.class));
		verify(client, times(1)).getLatestConfiguration(any(GetLatestConfigurationRequest.class));
	}

	@Test
	void shouldReuseSessionTokenOnSubsequentInit() {
		GetLatestConfigurationResponse response = buildResponse("key1=value1", "text/plain");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class)))
			.thenAnswer(invocation -> {
				GetLatestConfigurationRequest request = invocation.getArgument(0);
				assertThat(request.configurationToken()).isEqualTo("existing-token");
				return response;
			});

		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client, "existing-token",
				null);
		propertySource.init();

		// Should NOT call startConfigurationSession since token already exists
		verify(client, times(0)).startConfigurationSession(any(StartConfigurationSessionRequest.class));
		verify(client, times(1)).getLatestConfiguration(any(GetLatestConfigurationRequest.class));
	}

	@Test
	void shouldHandleEmptyConfigurationResponse() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
			.configuration(SdkBytes.fromByteArray(new byte[0]))
			.contentType("text/plain")
			.nextPollConfigurationToken("next-token")
			.build();

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class)))
			.thenAnswer(invocation -> {
				GetLatestConfigurationRequest request = invocation.getArgument(0);
				assertThat(request.configurationToken()).isEqualTo("initial-token");
				return response;
			});

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).isEmpty();
	}

	@Test
	void shouldUpdateNextPollTokenAfterInit() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse firstResponse = buildResponse("key1=value1", "text/plain", "next-token-1");
		GetLatestConfigurationResponse secondResponse = buildResponse("key2=value2", "text/plain", "next-token-2");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class)))
			.thenAnswer(invocation -> {
				GetLatestConfigurationRequest request = invocation.getArgument(0);
				assertThat(request.configurationToken()).isEqualTo("initial-token");
				return firstResponse;
			})
			.thenAnswer(invocation -> {
				GetLatestConfigurationRequest request = invocation.getArgument(0);
				assertThat(request.configurationToken()).isEqualTo("next-token-1");
				return secondResponse;
			});

		propertySource.init();
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");

		// Second init should use the next poll token from first response
		propertySource.init();
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void copyShouldPreserveSessionTokenAndProperties() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse(loadResource("test-config.properties"), "text/plain");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		AppConfigPropertySource copy = propertySource.copy();

		assertThat(copy.getName()).isEqualTo(propertySource.getName());
		assertThat(copy.getPropertyNames()).containsExactlyInAnyOrder(propertySource.getPropertyNames());
		assertThat(copy.getProperty("key1")).isEqualTo("value1");
		assertThat(copy.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void copyShouldNotCallStartSessionAgain() {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(DEFAULT_CONTEXT, client);

		GetLatestConfigurationResponse response = buildResponse("key1=value1", "text/plain");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		AppConfigPropertySource copy = propertySource.copy();

		GetLatestConfigurationResponse secondResponse = buildResponse("key2=value2", "text/plain", "next-token-2");

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(secondResponse);

		copy.init();

		verify(client, times(1)).startConfigurationSession(any(StartConfigurationSessionRequest.class));
	}

	@Test
	void shouldUseCorrectContextIdentifiers() {
		RequestContext context = new RequestContext("myProfile", "myEnv", "myApp", "myApp#myProfile#myEnv");
		AppConfigPropertySource propertySource = new AppConfigPropertySource(context, client);

		when(client.startConfigurationSession(any(StartConfigurationSessionRequest.class)))
			.thenAnswer(invocation -> {
				StartConfigurationSessionRequest request = invocation.getArgument(0);
				assertThat(request.applicationIdentifier()).isEqualTo("myApp");
				assertThat(request.environmentIdentifier()).isEqualTo("myEnv");
				assertThat(request.configurationProfileIdentifier()).isEqualTo("myProfile");
				return StartConfigurationSessionResponse.builder().initialConfigurationToken("token").build();
			});

		GetLatestConfigurationResponse response = GetLatestConfigurationResponse.builder()
			.configuration(SdkBytes.fromByteArray(new byte[0]))
			.contentType("text/plain")
			.nextPollConfigurationToken("next-token")
			.build();

		when(client.getLatestConfiguration(any(GetLatestConfigurationRequest.class))).thenReturn(response);

		propertySource.init();

		verify(client, times(1)).startConfigurationSession(any(StartConfigurationSessionRequest.class));
	}

	private GetLatestConfigurationResponse buildResponse(String content, String contentType) {
		return buildResponse(content, contentType, "next-token");
	}

	private GetLatestConfigurationResponse buildResponse(String content, String contentType, String nextToken) {
		return GetLatestConfigurationResponse.builder()
			.configuration(SdkBytes.fromUtf8String(content))
			.contentType(contentType)
			.nextPollConfigurationToken(nextToken)
			.build();
	}
}
