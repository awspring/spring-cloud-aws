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

package io.awspring.cloud.core.env.ec2;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashMap;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.util.EC2MetadataUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserDataPropertySource}
 *
 * @author Agim Emruli
 * @author Eddú Meléndez
 */
class UserDataPropertySourceTest {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;

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

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY,
				localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
	}

	@Test
	void getProperty_userDataWithDefaultFormatting_ReturnsUserDataKeys() throws Exception {
		// Arrange
		httpServer.createContext("/latest/user-data/",
				new StringWritingHttpHandler("keyA:valueA;keyB:valueB".getBytes("UTF-8")));

		// Act
		UserDataPropertySource userDataPropertySource = new UserDataPropertySource("test");

		// Assert
		assertThat(userDataPropertySource.getProperty("keyA")).isEqualTo("valueA");
		assertThat(userDataPropertySource.getProperty("keyB")).isEqualTo("valueB");

		httpServer.removeContext("/latest/user-data/");
	}

	@Test
	void getProperty_userDataWithCustomFormatting_ReturnsUserDataKeys() throws Exception {
		// Arrange
		httpServer.createContext("/latest/user-data/",
				new StringWritingHttpHandler("keyA=valueD,keyB=valueE".getBytes("UTF-8")));

		// Act
		UserDataPropertySource userDataPropertySource = new UserDataPropertySource("test");

		userDataPropertySource.setUserDataAttributeSeparator(",");
		userDataPropertySource.setUserDataValueSeparator("=");

		// Assert
		assertThat(userDataPropertySource.getProperty("keyA")).isEqualTo("valueD");
		assertThat(userDataPropertySource.getProperty("keyB")).isEqualTo("valueE");

		httpServer.removeContext("/latest/user-data/");
	}

	@AfterEach
	void clearMetadataCache() throws Exception {
		Field metadataCacheField = EC2MetadataUtils.class.getDeclaredField("cache");
		metadataCacheField.setAccessible(true);
		metadataCacheField.set(null, new HashMap<String, String>());
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
