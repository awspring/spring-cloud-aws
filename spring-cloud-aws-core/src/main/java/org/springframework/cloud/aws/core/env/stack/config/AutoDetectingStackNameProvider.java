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

package org.springframework.cloud.aws.core.env.stack.config;

import java.util.Collections;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.env.ec2.AmazonEc2InstanceIdProvider;
import org.springframework.cloud.aws.core.env.ec2.InstanceIdProvider;
import org.springframework.util.Assert;

/**
 * Represents a stack name provider that automatically detects the current stack name
 * based on the amazon elastic cloud environment.
 *
 * @author Christian Stettler
 * @author Agim Emruli
 */
public class AutoDetectingStackNameProvider
		implements StackNameProvider, InitializingBean {

	private final AmazonCloudFormation amazonCloudFormationClient;

	private final AmazonEC2 amazonEc2Client;

	private final InstanceIdProvider instanceIdProvider;

	private String stackName;

	private AutoDetectingStackNameProvider(
			AmazonCloudFormation amazonCloudFormationClient, AmazonEC2 amazonEc2Client,
			InstanceIdProvider instanceIdProvider) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
		this.amazonEc2Client = amazonEc2Client;
		this.instanceIdProvider = instanceIdProvider;
		afterPropertiesSet();
	}

	public AutoDetectingStackNameProvider(AmazonCloudFormation amazonCloudFormationClient,
			AmazonEC2 amazonEc2Client) {
		this(amazonCloudFormationClient, amazonEc2Client,
				new AmazonEc2InstanceIdProvider());
	}

	@Override
	public void afterPropertiesSet() {
		String instanceId = this.instanceIdProvider.getCurrentInstanceId();
		this.stackName = autoDetectStackName(instanceId);
		if (this.stackName == null) {
			throw new IllegalStateException(
					"No stack resources found in stack for EC2 instance '" + instanceId
							+ "'");
		}
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

	private String autoDetectStackName(String instanceId) {

		Assert.notNull(instanceId, "No valid instance id defined");
		DescribeStackResourcesResult describeStackResourcesResult = this.amazonCloudFormationClient
				.describeStackResources(new DescribeStackResourcesRequest()
						.withPhysicalResourceId(instanceId));

		if (describeStackResourcesResult != null
				&& describeStackResourcesResult.getStackResources() != null
				&& !describeStackResourcesResult.getStackResources().isEmpty()) {
			return describeStackResourcesResult.getStackResources().get(0).getStackName();
		}

		if (this.amazonEc2Client != null) {
			DescribeTagsResult describeTagsResult = this.amazonEc2Client
					.describeTags(new DescribeTagsRequest().withFilters(
							new Filter("resource-id",
									Collections.singletonList(instanceId)),
							new Filter("resource-type",
									Collections.singletonList("instance")),
							new Filter("key", Collections
									.singletonList("aws:cloudformation:stack-name"))));

			if (describeTagsResult != null && describeTagsResult.getTags() != null
					&& !describeTagsResult.getTags().isEmpty()) {
				return describeTagsResult.getTags().get(0).getValue();
			}
		}

		return null;
	}

}
