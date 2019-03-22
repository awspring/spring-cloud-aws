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

package org.springframework.cloud.aws.core.env.ec2;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceDataPropertySourceTest {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;

	@BeforeClass
	public static void setupHttpServer() throws Exception {
		InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
		httpServer = HttpServer.create(address, -1);
		httpServer.start();
		overwriteMetadataEndpointUrl(
				"http://" + address.getHostName() + ":" + address.getPort());
	}

	@AfterClass
	public static void shutdownHttpServer() throws Exception {
		if (httpServer != null) {
			httpServer.stop(10);
		}
		resetMetadataEndpointUrlOverwrite();
	}

	private static void overwriteMetadataEndpointUrl(
			String localMetadataServiceEndpointUrl) {
		System.setProperty(
				SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY,
				localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(
				SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
	}

	@Test
	public void getProperty_userDataWithDefaultFormatting_ReturnsUserDataKeys()
			throws Exception {
		// Arrange
		httpServer.createContext("/latest/user-data/", new StringWritingHttpHandler(
				"keyA:valueA;keyB:valueB".getBytes("UTF-8")));

		// Act
		AmazonEc2InstanceDataPropertySource amazonEc2InstanceDataPropertySource = new AmazonEc2InstanceDataPropertySource(
				"test");

		// Assert
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("keyA"))
				.isEqualTo("valueA");
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("keyB"))
				.isEqualTo("valueB");

		httpServer.removeContext("/latest/user-data/");
	}

	@Test
	public void getProperty_userDataWithCustomFormatting_ReturnsUserDataKeys()
			throws Exception {
		// Arrange
		httpServer.createContext("/latest/user-data/", new StringWritingHttpHandler(
				"keyA=valueD,keyB=valueE".getBytes("UTF-8")));

		// Act
		AmazonEc2InstanceDataPropertySource amazonEc2InstanceDataPropertySource = new AmazonEc2InstanceDataPropertySource(
				"test");

		amazonEc2InstanceDataPropertySource.setUserDataAttributeSeparator(",");
		amazonEc2InstanceDataPropertySource.setUserDataValueSeparator("=");

		// Assert
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("keyA"))
				.isEqualTo("valueD");
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("keyB"))
				.isEqualTo("valueE");

		httpServer.removeContext("/latest/user-data/");
	}

	@Test
	public void getProperty_knownAttribute_returnsAttributeValue() throws Exception {
		// Arrange
		httpServer.createContext("/latest/meta-data/instance-id",
				new StringWritingHttpHandler("i1234567".getBytes("UTF-8")));

		// Act
		AmazonEc2InstanceDataPropertySource amazonEc2InstanceDataPropertySource = new AmazonEc2InstanceDataPropertySource(
				"test");

		// Assert
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("instance-id"))
				.isEqualTo("i1234567");

		httpServer.removeContext("/latest/meta-data/instance-id");
	}

	@Test
	public void getProperty_knownAttributeWithSubAttribute_returnsAttributeValue()
			throws Exception {
		// Arrange
		httpServer.createContext("/latest/meta-data/services/domain",
				new StringWritingHttpHandler("amazonaws.com".getBytes("UTF-8")));

		// Act
		AmazonEc2InstanceDataPropertySource amazonEc2InstanceDataPropertySource = new AmazonEc2InstanceDataPropertySource(
				"test");

		// Assert
		assertThat(amazonEc2InstanceDataPropertySource.getProperty("services/domain"))
				.isEqualTo("amazonaws.com");

		httpServer.removeContext("/latest/meta-data/services/domain");
	}

	@Test
	public void getProperty_unknownAttribute_returnsNull() throws Exception {
		// Arrange
		httpServer.createContext("/latest/meta-data/instance-id",
				new StringWritingHttpHandler("i1234567".getBytes("UTF-8")));

		// Act
		AmazonEc2InstanceDataPropertySource amazonEc2InstanceDataPropertySource = new AmazonEc2InstanceDataPropertySource(
				"test");

		// Assert
		assertThat(
				amazonEc2InstanceDataPropertySource.getProperty("non-existing-attribute"))
						.isNull();

		httpServer.removeContext("/latest/meta-data/instance-id");
	}

	@After
	public void clearMetadataCache() throws Exception {
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
