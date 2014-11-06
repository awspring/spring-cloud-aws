/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.config;

import com.amazonaws.SDKGlobalConfiguration;
import com.sun.net.httpserver.HttpServer;
import org.springframework.util.SocketUtils;

import java.net.InetSocketAddress;

/**
 * @author Agim Emruli
 */
public final class InstanceIdServer {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;

	private InstanceIdServer(){
		// Avoid instantiation
	}

	@SuppressWarnings("NonThreadSafeLazyInitialization")
	public static HttpServer setupHttpServer() throws Exception {

		if (httpServer == null) {
			InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
			httpServer = HttpServer.create(address, -1);
			httpServer.start();
			overwriteMetadataEndpointUrl("http://" + address.getHostName() + ":" + address.getPort());
		}

		return httpServer;
	}

	public static void shutdownHttpServer() {
		if (httpServer != null) {
			httpServer.stop(10);
			httpServer = null;
		}
		resetMetadataEndpointUrlOverwrite();
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY, localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
	}
}