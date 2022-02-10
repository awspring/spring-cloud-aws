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

package io.awspring.cloud.v3.it.support;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;

/**
 * Retrieves the instance id output value from the configured stack and exposes it via a
 * local metadata service. In addition, sets the AWS SDK internal system property for
 * overwriting the service endpoint url used by the
 * {@link com.amazonaws.util.EC2MetadataUtils} to the url of the local metadata service.
 *
 * @author Christian Stettler
 */
public final class TestStackInstanceIdService {

	private static final String INSTANCE_ID_SERVICE_HOSTNAME = "localhost";

	private static final int INSTANCE_ID_SERVICE_PORT = 12345;

	private final InstanceIdSource instanceIdSource;

	private HttpServer httpServer;

	private TestStackInstanceIdService(InstanceIdSource instanceIdSource) {
		this.instanceIdSource = instanceIdSource;
	}

	public static TestStackInstanceIdService fromStackOutputKey(String stackName, String outputKey,
			CloudFormationClient amazonCloudFormationClient) {
		return new TestStackInstanceIdService(
				new AmazonStackOutputBasedInstanceIdSource(stackName, outputKey, amazonCloudFormationClient));
	}

	public static TestStackInstanceIdService fromInstanceId(String instanceId) {
		return new TestStackInstanceIdService(new StaticInstanceIdSource(instanceId));
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
				localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
	}

	private static void clearMetadataCache() {
		try {

			Field metadataCacheField = EC2MetadataUtils.class.getDeclaredField("cache");
			metadataCacheField.setAccessible(true);
			metadataCacheField.set(null, new HashMap<String, String>());
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Unable to clear metadata cache in '" + EC2MetadataUtils.class.getName() + "'", e);
		}
	}

	public void enable() {
		startMetadataHttpServer(this.instanceIdSource.getInstanceId());
		overwriteMetadataEndpointUrl("http://" + INSTANCE_ID_SERVICE_HOSTNAME + ":" + INSTANCE_ID_SERVICE_PORT);
	}

	public void disable() {
		resetMetadataEndpointUrlOverwrite();
		clearMetadataCache();
		stopMetadataHttpServer();
	}

	private void startMetadataHttpServer(String instanceId) {
		try {
			this.httpServer = HttpServer
					.create(new InetSocketAddress(INSTANCE_ID_SERVICE_HOSTNAME, INSTANCE_ID_SERVICE_PORT), -1);
			this.httpServer.createContext("/latest/meta-data/instance-id", new InstanceIdHttpHandler(instanceId));
			this.httpServer.start();
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to start metadata http server", e);
		}
	}

	private void stopMetadataHttpServer() {
		if (this.httpServer != null) {
			this.httpServer.stop(0);
		}
	}

	/**
	 * Utility interface for abstracting source for instance id.
	 */
	private interface InstanceIdSource {

		String getInstanceId();

	}

	/**
	 * Handler for responding with specified instance id
	 */
	private static final class InstanceIdHttpHandler implements HttpHandler {

		private final String instanceId;

		private InstanceIdHttpHandler(String instanceId) {
			this.instanceId = instanceId;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			httpExchange.sendResponseHeaders(200, this.instanceId.getBytes().length);

			OutputStream responseBody = httpExchange.getResponseBody();
			responseBody.write(this.instanceId.getBytes());
			responseBody.flush();
			responseBody.close();
		}

	}

	/**
	 * Source for statically configured instance id.
	 * <p>
	 * Useful for unit testing.
	 * </p>
	 */
	private static final class StaticInstanceIdSource implements InstanceIdSource {

		private final String instanceId;

		private StaticInstanceIdSource(String instanceId) {
			this.instanceId = instanceId;
		}

		@Override
		public String getInstanceId() {
			return this.instanceId;
		}

	}

	/**
	 * Source for retrieving instance id from specified output key of specified stack.
	 * Requires specified stack to be available.
	 * <p>
	 * Useful for integration testing.
	 * </p>
	 */
	private static final class AmazonStackOutputBasedInstanceIdSource implements InstanceIdSource {

		private final String stackName;

		private final String outputKey;

		private final CloudFormationClient amazonCloudFormationClient;

		private AmazonStackOutputBasedInstanceIdSource(String stackName, String outputKey,
													   CloudFormationClient amazonCloudFormationClient) {
			this.stackName = stackName;
			this.outputKey = outputKey;
			this.amazonCloudFormationClient = amazonCloudFormationClient;
		}

		private static Stack getStack(DescribeStacksResponse describeStacksResult, String stackName) {
			for (Stack stack : describeStacksResult.stacks()) {
				if (stack.stackName().equals(stackName)) {
					return stack;
				}
			}

			throw new IllegalStateException("No stack found with name '" + stackName + "' (available stacks: "
					+ allStackNames(describeStacksResult) + ")");
		}

		private static String getOutputValue(Stack stack, String outputKey) {
			for (Output output : stack.outputs()) {
				if (output.outputKey().equals(outputKey)) {
					return output.outputValue();
				}
			}

			throw new IllegalStateException(
					"No output '" + outputKey + "' defined in stack '" + stack.stackName() + "'");
		}

		private static List<String> allStackNames(DescribeStacksResponse describeStacksResult) {
			return describeStacksResult.stacks().stream()
				.map(Stack::stackName)
				.collect(Collectors.toList());
		}

		@Override
		public String getInstanceId() {
			DescribeStacksResponse describeStacksResult = this.amazonCloudFormationClient
					.describeStacks(DescribeStacksRequest.builder().build());
			Stack stack = getStack(describeStacksResult, this.stackName);

			return getOutputValue(stack, this.outputKey);
		}

	}

}
