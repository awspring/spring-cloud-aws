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

package org.elasticspring.core.env.stack.config;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import org.elasticspring.core.env.ec2.InstanceIdProvider;
import org.springframework.beans.factory.InitializingBean;

/**
 * Represents a stack name provider that automatically detects the current stack name based on the amazon elastic cloud
 * environment.
 *
 * @author Christian Stettler
 */
class AutoDetectingStackNameProvider implements StackNameProvider, InitializingBean {

	private final AmazonCloudFormationClient amazonCloudFormationClient;
	private final InstanceIdProvider instanceIdProvider;

	private String stackName;

	AutoDetectingStackNameProvider(AmazonCloudFormationClient amazonCloudFormationClient, InstanceIdProvider instanceIdProvider) {
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
