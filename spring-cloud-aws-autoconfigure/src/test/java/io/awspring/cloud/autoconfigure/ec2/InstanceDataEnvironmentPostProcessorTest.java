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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceDataEnvironmentPostProcessor}
 *
 * @author Eddú Meléndez
 */
class InstanceDataEnvironmentPostProcessorTest {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	private static HttpServer httpServer;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(context -> new InstanceDataEnvironmentPostProcessor()
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

	@Test
	void ec2InstanceDataEnabled() {
		init("/latest/meta-data/instance-id", "spring-cloud-instance");
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.instance-data.enabled:true").run(context -> {
			assertThat(context.getEnvironment().getProperty("instance-id")).isEqualTo("spring-cloud-instance");
		});
	}

	@Test
	void ec2UserDataDisabledByDefault() {
		this.contextRunner.run(context -> {
			assertThat(context.getEnvironment().containsProperty("instance-id")).isFalse();
		});
	}

	@Test
	void ec2UserDataDisabledByNotHavingCoreModule() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.instance-data.enabled:true")
				.withClassLoader(new FilteredClassLoader("io.awspring.cloud.core.env.ec2.InstanceDataPropertySource"))
				.run(context -> {
					assertThat(context.getEnvironment().containsProperty("instance-id")).isFalse();
				});
	}

	@Test
	void ec2UserDataDisabledByNotHavingAwsSdk() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.ec2.instance-data.enabled:true")
				.withClassLoader(new FilteredClassLoader("com.amazonaws.util.EC2MetadataUtils")).run(context -> {
					assertThat(context.getEnvironment().containsProperty("instance-id")).isFalse();
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
