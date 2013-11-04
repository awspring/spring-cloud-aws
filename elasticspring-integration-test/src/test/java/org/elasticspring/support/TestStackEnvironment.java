/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.support;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 */
public class TestStackEnvironment implements InitializingBean, DisposableBean {

	public static final String DEFAULT_STACK_NAME = "IntegrationTestStack";
	private static final String TEMPLATE_PATH = "IntegrationTest.template";

	private final AmazonCloudFormation amazonCloudFormation;
	private DescribeStackResourcesResult stackResources;
	private boolean stackCreatedByThisInstance;

	@Autowired
	public TestStackEnvironment(AWSCredentialsProvider awsCredentialsProvider) {
		this.amazonCloudFormation = new AmazonCloudFormationClient(awsCredentialsProvider);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.stackResources = getStackResources(DEFAULT_STACK_NAME);
	}

	private DescribeStackResourcesResult getStackResources(String stackName) throws InterruptedException, IOException {
		try {
			DescribeStacksResult describeStacksResult = this.amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackName));
			for (Stack stack : describeStacksResult.getStacks()) {
				if (isAvailable(stack)) {
					return this.amazonCloudFormation.describeStackResources(new DescribeStackResourcesRequest().withStackName(stack.getStackName()));
				}
				if (isError(stack)) {
					if (this.stackCreatedByThisInstance) {
						throw new IllegalArgumentException("Could not create stack");
					}
					this.amazonCloudFormation.deleteStack(new DeleteStackRequest().withStackName(stack.getStackName()));
					return getStackResources(stackName);
				}
				if (isInProgress(stack)) {
					//noinspection BusyWait
					Thread.sleep(5000L);
					return getStackResources(stackName);
				}
			}
		} catch (AmazonClientException e) {
			String templateBody = FileCopyUtils.copyToString(new InputStreamReader(new ClassPathResource(TEMPLATE_PATH).getInputStream()));
			this.amazonCloudFormation.createStack(new CreateStackRequest().withTemplateBody(templateBody).withOnFailure(OnFailure.DO_NOTHING).
					withStackName(stackName));
			this.stackCreatedByThisInstance = true;
		}

		return getStackResources(stackName);
	}

	public String getByLogicalId(String id) {
		for (StackResource stackResource : this.stackResources.getStackResources()) {
			if (stackResource.getLogicalResourceId().equals(id)) {
				return stackResource.getPhysicalResourceId();
			}
		}
		return null;
	}

	private static boolean isInProgress(Stack stack) {
		return stack.getStackStatus().endsWith("_PROGRESS");
	}

	private boolean isAvailable(Stack stack) {
		return stack.getStackStatus().endsWith("_COMPLETE") && !"DELETE_COMPLETE".equals(stack.getStackStatus());
	}

	private boolean isError(Stack stack) {
		return stack.getStackStatus().endsWith("_FAILED");
	}

	@Override
	public void destroy() throws Exception {
		if (this.stackCreatedByThisInstance) {
			this.amazonCloudFormation.deleteStack(new DeleteStackRequest().withStackName(DEFAULT_STACK_NAME));
		}
	}
}