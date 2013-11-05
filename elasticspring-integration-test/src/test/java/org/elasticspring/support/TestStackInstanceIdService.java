package org.elasticspring.support;

import com.amazonaws.internal.EC2MetadataClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves the instance id output value from the configured stack and exposes it via a local metadata service. In
 * addition, sets the AWS SDK internal system property for overwriting the service endpoint url used by the {@link
 * com.amazonaws.util.EC2MetadataUtils} to the url of the local metadata service.
 *
 * @author Christian Stettler
 */
public class TestStackInstanceIdService implements InitializingBean, DisposableBean {

	public static final String INSTANCE_ID_SERVICE_HOSTNAME = "localhost";
	public static final int INSTANCE_ID_SERVICE_PORT = 12345;
	public static final String INSTANCE_ID_STACK_OUTPUT_KEY = "InstanceId";

	private final String stackName;
	private final AmazonCloudFormationClient amazonCloudFormationClient;

	private HttpServer httpServer;

	public TestStackInstanceIdService(String stackName, AmazonCloudFormationClient amazonCloudFormationClient) {
		this.stackName = stackName;
		this.amazonCloudFormationClient = amazonCloudFormationClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		DescribeStacksResult describeStacksResult = this.amazonCloudFormationClient.describeStacks(new DescribeStacksRequest());

		Stack stack = getStack(describeStacksResult, this.stackName);
		String instanceId = getOutputValue(stack, INSTANCE_ID_STACK_OUTPUT_KEY);

		this.httpServer = HttpServer.create(new InetSocketAddress(INSTANCE_ID_SERVICE_HOSTNAME, INSTANCE_ID_SERVICE_PORT), -1);
		this.httpServer.createContext("/latest/meta-data/instance-id", new InstanceIdHttpHandler(instanceId));
		this.httpServer.start();

		overwriteMetadataEndpointUrl("http://" + INSTANCE_ID_SERVICE_HOSTNAME + ":" + INSTANCE_ID_SERVICE_PORT);
	}

	@Override
	public void destroy() throws Exception {
		resetMetadataEndpointUrlOverwrite();

		if (this.httpServer != null) {
			this.httpServer.stop(0);
		}
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

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(EC2MetadataClient.EC2_METADATA_SERVICE_OVERRIDE, localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(EC2MetadataClient.EC2_METADATA_SERVICE_OVERRIDE);
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

}
