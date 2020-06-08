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

import com.amazonaws.SDKGlobalConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SocketUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Agim Emruli
 */

@ExtendWith(SpringExtension.class)
public abstract class AmazonEc2InstanceDataPropertySourceAwsTest {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();
	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;
	@Autowired
	private SimpleConfigurationBean simpleConfigurationBean;

	@BeforeAll
	static void setupHttpServer() throws Exception {
		InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
		httpServer = HttpServer.create(address, -1);
		httpServer.createContext("/latest/user-data", new StringWritingHttpHandler(
				"key1:value1;key2:value2;key3:value3".getBytes()));
		httpServer.createContext("/latest/meta-data/instance-id",
				new StringWritingHttpHandler("i123456".getBytes()));
		httpServer.start();
		overwriteMetadataEndpointUrl(
				"http://" + address.getHostName() + ":" + address.getPort());
		restContextInstanceDataCondition();
	}

	@AfterAll
	static void shutdownHttpServer() throws Exception {
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

	private static void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class,
				"isCloudEnvironment");
		assertNotNull(field);
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@Test
	void testInstanceDataResolution() throws Exception {
		Assertions.assertEquals("value1", this.simpleConfigurationBean.getValue1());
		Assertions.assertEquals("value2", this.simpleConfigurationBean.getValue2());
		Assertions.assertEquals("value3", this.simpleConfigurationBean.getValue3());
		Assertions.assertEquals("i123456", this.simpleConfigurationBean.getValue4());
	}

	private static class StringWritingHttpHandler implements HttpHandler {

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
