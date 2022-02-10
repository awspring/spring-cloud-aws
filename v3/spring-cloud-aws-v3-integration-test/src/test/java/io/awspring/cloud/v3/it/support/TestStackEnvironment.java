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
import java.io.InputStreamReader;

import com.amazonaws.AmazonClientException;
import io.awspring.cloud.core.env.ec2.InstanceIdProvider;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.OnFailure;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.Tag;

/**
 *
 */
public class TestStackEnvironment implements InitializingBean, DisposableBean, InstanceIdProvider {

	public static final String DEFAULT_STACK_NAME = "IntegrationTestStack";

	public static final String INSTANCE_ID_STACK_OUTPUT_KEY = "InstanceId";

	private static final String EC2_INSTANCE_NAME = "UserTagAndUserDataInstance";

	private static final String TEMPLATE_PATH = "IntegrationTestStack.yaml";

	private final CloudFormationClient amazonCloudFormationClient;

	@Value("${rdsPassword}")
	private String rdsPassword;

	private DescribeStackResourcesResponse stackResources;

	private boolean stackCreatedByThisInstance;

	@Autowired
	public TestStackEnvironment(CloudFormationClient amazonCloudFormationClient) {
		this.amazonCloudFormationClient = amazonCloudFormationClient;
	}

	private static boolean isInProgress(Stack stack) {
		return stack.stackStatusAsString().endsWith("_PROGRESS");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stackResources = getStackResources(DEFAULT_STACK_NAME);
	}

	@Override
	public String getCurrentInstanceId() {
		return getByLogicalId(EC2_INSTANCE_NAME);
	}

	private DescribeStackResourcesResponse getStackResources(String stackName) throws InterruptedException, IOException {
		try {
			DescribeStacksResponse describeStackResourcesResponse = this.amazonCloudFormationClient
				.describeStacks(DescribeStacksRequest.builder()
					.stackName(stackName)
					.build());
			for (Stack stack : describeStackResourcesResponse.stacks()) {
				if (isAvailable(stack)) {
					return this.amazonCloudFormationClient.describeStackResources(
						DescribeStackResourcesRequest.builder()
							.stackName(stack.stackName())
							.build());
				}
				if (isError(stack)) {
					if (this.stackCreatedByThisInstance) {
						throw new IllegalArgumentException("Could not create stack");
					}
					this.amazonCloudFormationClient
							.deleteStack(DeleteStackRequest.builder()
								.stackName(stack.stackName())
								.build());
					return getStackResources(stackName);
				}
				if (isInProgress(stack)) {
					// noinspection BusyWait
					Thread.sleep(5000L);
					return getStackResources(stackName);
				}
			}
		}
		catch (AmazonClientException e) {
			String templateBody = FileCopyUtils
					.copyToString(new InputStreamReader(new ClassPathResource(TEMPLATE_PATH).getInputStream()));
			this.amazonCloudFormationClient.createStack(
				CreateStackRequest.builder()
					.templateBody(templateBody)
					.onFailure(OnFailure.DELETE)
					.stackName(stackName)
					.tags(Tag.builder()
						.key("tag1")
						.value("value1")
						.build())
					.parameters(Parameter.builder()
						.parameterKey("RdsPassword")
						.parameterValue(this.rdsPassword)
						.build())
					.build());
			this.stackCreatedByThisInstance = true;
		}

		return getStackResources(stackName);
	}

	protected String getByLogicalId(String id) {
		for (StackResource stackResource : this.stackResources.stackResources()) {
			if (stackResource.logicalResourceId().equals(id)) {
				return stackResource.physicalResourceId();
			}
		}
		return null;
	}

	private boolean isAvailable(Stack stack) {
		return stack.stackStatusAsString().toString().endsWith("_COMPLETE")
			&& !"DELETE_COMPLETE".equals(stack.stackStatusAsString());
	}

	private boolean isError(Stack stack) {
		return stack.stackStatusAsString().endsWith("_FAILED");
	}

	@Override
	public void destroy() throws Exception {
		if (this.stackCreatedByThisInstance) {
			this.amazonCloudFormationClient.deleteStack(DeleteStackRequest.builder()
					.stackName(DEFAULT_STACK_NAME)
				.build());
		}
	}

	public boolean isStackCreatedAutomatically() {
		return this.stackCreatedByThisInstance;
	}

}
