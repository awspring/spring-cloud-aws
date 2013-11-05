package org.elasticspring.support;

import com.amazonaws.internal.EC2MetadataClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.util.EC2MetadataUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Retrieves the instance id output value from the configured stack and exposes it via a local metadata service. In
 * addition, sets the AWS SDK internal system property for overwriting the service endpoint url used by the {@link
 * com.amazonaws.util.EC2MetadataUtils} to the url of the local metadata service.
 *
 * @author Christian Stettler
 */
public class TestStackInstanceIdService {

	public static final String INSTANCE_ID_SERVICE_HOSTNAME = "localhost";
	public static final int INSTANCE_ID_SERVICE_PORT = 12345;

	private final InstanceIdSource instanceIdSource;

	private HttpServer httpServer;

	private TestStackInstanceIdService(InstanceIdSource instanceIdSource) {
		this.instanceIdSource = instanceIdSource;
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
			this.httpServer = HttpServer.create(new InetSocketAddress(INSTANCE_ID_SERVICE_HOSTNAME, INSTANCE_ID_SERVICE_PORT), -1);
			this.httpServer.createContext("/latest/meta-data/instance-id", new InstanceIdHttpHandler(instanceId));
			this.httpServer.start();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to start metadata http server", e);
		}
	}

	private void stopMetadataHttpServer() {
		if (this.httpServer != null) {
			this.httpServer.stop(0);
		}
	}

	public static TestStackInstanceIdService fromStackOutputKey(String stackName, String outputKey, AmazonCloudFormationClient amazonCloudFormationClient) {
		return new TestStackInstanceIdService(new AmazonStackOutputBasedInstanceIdSource(stackName, outputKey, amazonCloudFormationClient));
	}

	public static TestStackInstanceIdService fromInstanceId(String instanceId) {
		return new TestStackInstanceIdService(new StaticInstanceIdSource(instanceId));
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(EC2MetadataClient.EC2_METADATA_SERVICE_OVERRIDE, localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(EC2MetadataClient.EC2_METADATA_SERVICE_OVERRIDE);
	}

	private static void clearMetadataCache() {
		try {
			Field metadataCacheField = EC2MetadataUtils.class.getDeclaredField("cache");
			metadataCacheField.setAccessible(true);
			metadataCacheField.set(null, new HashMap<String, String>());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to clear metadata cache in '" + EC2MetadataUtils.class.getName() + "'", e);
		}
	}


	/**
	 * Handler for responding with specified instance id
	 */
	private static class InstanceIdHttpHandler implements HttpHandler {

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
	 * Utility interface for abstracting source for instance id.
	 */
	private interface InstanceIdSource {

		String getInstanceId();

	}


	/**
	 * Source for statically configured instance id.
	 * <p/>
	 * Useful for unit testing.
	 */
	private static class StaticInstanceIdSource implements InstanceIdSource {

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
	 * Source for retrieving instance id from specified output key of specified stack. Requires specified stack to be
	 * available.
	 * <p/>
	 * Useful for integration testing.
	 */
	private static class AmazonStackOutputBasedInstanceIdSource implements InstanceIdSource {

		private final String stackName;
		private final String outputKey;
		private final AmazonCloudFormationClient amazonCloudFormationClient;

		private AmazonStackOutputBasedInstanceIdSource(String stackName, String outputKey, AmazonCloudFormationClient amazonCloudFormationClient) {
			this.stackName = stackName;
			this.outputKey = outputKey;
			this.amazonCloudFormationClient = amazonCloudFormationClient;
		}

		@Override
		public String getInstanceId() {
			DescribeStacksResult describeStacksResult = this.amazonCloudFormationClient.describeStacks(new DescribeStacksRequest());
			Stack stack = getStack(describeStacksResult, this.stackName);

			return getOutputValue(stack, this.outputKey);
		}

		private static Stack getStack(DescribeStacksResult describeStacksResult, String stackName) {
			for (Stack stack : describeStacksResult.getStacks()) {
				if (stack.getStackName().equals(stackName)) {
					return stack;
				}
			}

			throw new IllegalStateException("No stack found with name '" + stackName + "' (available stacks: " + allStackNames(describeStacksResult) + ")");
		}

		private static String getOutputValue(Stack stack, String outputKey) {
			for (Output output : stack.getOutputs()) {
				if (output.getOutputKey().equals(outputKey)) {
					return output.getOutputValue();
				}
			}

			throw new IllegalStateException("No output '" + outputKey + "' defined in stack '" + stack.getStackName() + "'");
		}

		private static List<String> allStackNames(DescribeStacksResult describeStacksResult) {
			List<String> allStackNames = new ArrayList<String>();

			for (Stack stack : describeStacksResult.getStacks()) {
				allStackNames.add(stack.getStackName());
			}

			return allStackNames;
		}

	}

}
