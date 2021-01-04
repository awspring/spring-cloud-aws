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

package io.awspring.cloud.autoconfigure.ec2;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.amazonaws.SDKGlobalConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserDataEnvironmentPostProcessor}
 *
 * @author Eddú Meléndez
 */
class UserDataEnvironmentPostProcessorTest {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	private static HttpServer httpServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(context -> new UserDataEnvironmentPostProcessor()
					.postProcessEnvironment(context.getEnvironment(), new SpringApplication()));

	@BeforeAll
	static void setupHttpServer() throws Exception {
		InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
		httpServer = HttpServer.create(address, -1);
		httpServer.start();
		overwriteMetadataEndpointUrl("http://" + address.getHostName() + ":" + address.getPort());
	}

	@AfterAll
	static void shutdownHttpServer() throws Exception {
		if (httpServer != null) {
			httpServer.stop(10);
		}
		resetMetadataEndpointUrlOverwrite();
	}

	@AfterEach
	void removeContext() {
		httpServer.removeContext("/latest/user-data/");
	}

	@Test
	void ec2UserDataEnabled() {
		init("/latest/user-data/", "keyA:valueA;keyB:valueB");
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.user-data.enabled:true").run(context -> {
			assertThat(context.getEnvironment().getProperty("keyA")).isEqualTo("valueA");
			assertThat(context.getEnvironment().getProperty("keyB")).isEqualTo("valueB");
		});
	}

	@Test
	void ec2UserDataEnabledWithCustomSeparators() {
		init("/latest/user-data/", "keyA=valueA,keyB=valueB");
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.user-data.enabled:true",
				"spring.cloud.aws.ec2.user-data.attribute-separator:,",
				"spring.cloud.aws.ec2.user-data.value-separator:=").run(context -> {
					assertThat(context.getEnvironment().getProperty("keyA")).isEqualTo("valueA");
					assertThat(context.getEnvironment().getProperty("keyB")).isEqualTo("valueB");
				});
	}

	@Test
	void ec2UserDataDisabledByDefault() {
		init("/latest/user-data/", "keyA:valueA;keyB:valueB");
		this.contextRunner.run(context -> {
			assertThat(context.getEnvironment().containsProperty("keyA")).isFalse();
			assertThat(context.getEnvironment().containsProperty("keyB")).isFalse();
		});
	}

	@Test
	void ec2UserDataDisabledByNotHavingCoreModule() {
		init("/latest/user-data/", "keyA:valueA;keyB:valueB");
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.user-data.enabled:true")
				.withClassLoader(new FilteredClassLoader("io.awspring.cloud.core.env.ec2.UserDataPropertySource"))
				.run(context -> {
					assertThat(context.getEnvironment().containsProperty("keyA")).isFalse();
					assertThat(context.getEnvironment().containsProperty("keyB")).isFalse();
				});
	}

	@Test
	void ec2UserDataDisabledByNotHavingAwsSdk() {
		init("/latest/user-data/", "keyA:valueA;keyB:valueB");
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.user-data.enabled:true")
				.withClassLoader(new FilteredClassLoader("com.amazonaws.util.EC2MetadataUtils")).run(context -> {
					assertThat(context.getEnvironment().containsProperty("keyA")).isFalse();
					assertThat(context.getEnvironment().containsProperty("keyB")).isFalse();
				});
	}

	private static void init(String path, String data) {
		httpServer.createContext(path, new StringWritingHttpHandler(data.getBytes(StandardCharsets.UTF_8)));
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY,
				localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
	}

	private static final class StringWritingHttpHandler implements HttpHandler {

		private final byte[] content;

		private StringWritingHttpHandler(byte[] content) {
			this.content = content;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			httpExchange.sendResponseHeaders(200, this.content.length);
			OutputStream responseBody = httpExchange.getResponseBody();
			responseBody.write(this.content);
			responseBody.flush();
			responseBody.close();
		}

	}

}
