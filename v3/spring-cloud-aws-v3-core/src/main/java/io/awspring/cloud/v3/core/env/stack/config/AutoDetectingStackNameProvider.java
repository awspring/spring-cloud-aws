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

package io.awspring.cloud.v3.core.env.stack.config;

import java.util.Collections;
import java.util.Objects;

import io.awspring.cloud.v3.core.env.ec2.AmazonEc2InstanceIdProvider;
import io.awspring.cloud.v3.core.env.ec2.InstanceIdProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;

/**
 * Represents a stack name provider that automatically detects the current stack name
 * based on the amazon elastic cloud environment.
 *
 * @author Christian Stettler
 * @author Agim Emruli
 */
public class AutoDetectingStackNameProvider implements StackNameProvider, InitializingBean {

	private final CloudFormationClient amazonCloudFormationClient;

	private final Ec2Client amazonEc2Client;

	private final InstanceIdProvider instanceIdProvider;

	private String stackName;

	private AutoDetectingStackNameProvider(CloudFormationClient amazonCloudFormationClient, Ec2Client amazonEc2Client,
			InstanceIdProvider instanceIdProvider) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
		this.amazonEc2Client = amazonEc2Client;
		this.instanceIdProvider = instanceIdProvider;
		afterPropertiesSet();
	}

	public AutoDetectingStackNameProvider(CloudFormationClient amazonCloudFormationClient, Ec2Client amazonEc2Client) {
		this(amazonCloudFormationClient, amazonEc2Client, new AmazonEc2InstanceIdProvider());
	}

	@Override
	public void afterPropertiesSet() {
		String instanceId = this.instanceIdProvider.getCurrentInstanceId();
		this.stackName = autoDetectStackName(instanceId);
		if (this.stackName == null) {
			throw new IllegalStateException("No stack resources found in stack for EC2 instance '" + instanceId + "'");
		}
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

	private String autoDetectStackName(String instanceId) {
		Assert.notNull(instanceId, "No valid instance id defined");
		DescribeStackResourcesResponse describeStackResourcesResult = this.amazonCloudFormationClient
				.describeStackResources(
					DescribeStackResourcesRequest.builder()
						.physicalResourceId(instanceId)
						.build());

		if (describeStackResourcesResult != null && describeStackResourcesResult.stackResources() != null
				&& !describeStackResourcesResult.stackResources().isEmpty()) {
			return describeStackResourcesResult.stackResources().get(0).stackName();
		}

		if (this.amazonEc2Client != null) {
			DescribeTagsResponse describeTagsResult = this.amazonEc2Client.describeTags(
				DescribeTagsRequest.builder()
					.filters(Filter.builder()
						.name("resource-id")
						.values(instanceId)
						.build(),
						Filter.builder()
							.name("resource-type")
							.values("instance")
							.build(),
						Filter.builder()
							.name("key")
							.values("aws:cloudformation:stack-name")
							.build())
					.build());



			if (Objects.nonNull(describeTagsResult) && !CollectionUtils.isEmpty(describeTagsResult.tags())) {
				return describeTagsResult.tags().get(0).value();
			}
		}

		return null;
	}

}
