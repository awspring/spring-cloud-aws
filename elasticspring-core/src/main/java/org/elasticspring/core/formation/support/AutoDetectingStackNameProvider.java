package org.elasticspring.core.formation.support;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import org.elasticspring.core.env.ec2.InstanceIdProvider;
import org.elasticspring.core.formation.StackNameProvider;
import org.springframework.beans.factory.InitializingBean;

/**
 * Represents a stack name provider that automatically detects the current stack name based on the amazon elastic cloud
 * environment.
 *
 * @author Christian Stettler
 */
public class AutoDetectingStackNameProvider implements StackNameProvider, InitializingBean {

	private final AmazonCloudFormationClient amazonCloudFormationClient;
	private final InstanceIdProvider instanceIdProvider;

	private String stackName;

	public AutoDetectingStackNameProvider(AmazonCloudFormationClient amazonCloudFormationClient, InstanceIdProvider instanceIdProvider) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
		this.instanceIdProvider = instanceIdProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stackName = autoDetectStackName(this.amazonCloudFormationClient, this.instanceIdProvider.getCurrentInstanceId());
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

	private static String autoDetectStackName(AmazonCloudFormationClient amazonCloudFormationClient, String instanceId) {
		DescribeStackResourcesResult describeStackResourcesResult = amazonCloudFormationClient.describeStackResources(new DescribeStackResourcesRequest().withPhysicalResourceId(instanceId));

		if (describeStackResourcesResult.getStackResources().isEmpty()) {
			throw new IllegalStateException("No stack resources found in stack for EC2 instance '" + instanceId + "'");
		}

		return describeStackResourcesResult.getStackResources().get(0).getStackName();
	}

}
