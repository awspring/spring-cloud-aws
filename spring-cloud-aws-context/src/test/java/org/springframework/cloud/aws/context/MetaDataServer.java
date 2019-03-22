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

package org.springframework.cloud.aws.context;

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

import org.springframework.util.SocketUtils;

/**
 * @author Agim Emruli
 */
public final class MetaDataServer {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;

	private MetaDataServer() {
		// Avoid instantiation
	}

	@SuppressWarnings("NonThreadSafeLazyInitialization")
	public static HttpServer setupHttpServer() throws Exception {

		if (httpServer == null) {
			InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
			httpServer = HttpServer.create(address, -1);
			httpServer.start();
			overwriteMetadataEndpointUrl(
					"http://" + address.getHostName() + ":" + address.getPort());
		}

		return httpServer;
	}

	public static void shutdownHttpServer() {
		if (httpServer != null) {
			httpServer.stop(10);
			httpServer = null;
		}
		resetMetadataEndpointUrlOverwrite();
		resetMetaDataCache();
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

	private static void resetMetaDataCache() {
		try {
			Field metadataCacheField = EC2MetadataUtils.class.getDeclaredField("cache");
			metadataCacheField.setAccessible(true);
			metadataCacheField.set(null, new HashMap<String, String>());
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to clear metadata cache in '"
					+ EC2MetadataUtils.class.getName() + "'", e);
		}
	}

	public static class HttpResponseWriterHandler implements HttpHandler {

		private final String content;

		public HttpResponseWriterHandler(String content) {
			this.content = content;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			httpExchange.sendResponseHeaders(200, this.content.getBytes().length);

			OutputStream responseBody = httpExchange.getResponseBody();
			responseBody.write(this.content.getBytes());
			responseBody.flush();
			responseBody.close();
		}

	}

}
