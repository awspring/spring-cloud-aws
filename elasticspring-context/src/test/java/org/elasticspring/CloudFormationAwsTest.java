/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CloudFormationAwsTest {

	private AmazonCloudFormation amazonCloudFormation;

	@Value("#{credentials.accessKey}")
	private String accessKey;

	@Value("#{credentials.secretKey}")
	private String secretKey;

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testGetConfiguration() throws Exception {
		this.amazonCloudFormation = new AmazonCloudFormationClient(new BasicAWSCredentials(this.accessKey, this.secretKey));
		this.amazonCloudFormation.setEndpoint("https://cloudformation.us-east-1.amazonaws.com");

//		CreateStackResult sample = this.amazonCloudFormation.createStack(new CreateStackRequest().withStackName("sample").withTemplateURL("https://s3.amazonaws.com/cloudformation-templates-us-east-1/ElasticBeanstalk-1.0.0.template"));
//		System.out.println("sample = " + sample);

		DescribeStacksResult stacksResult = this.amazonCloudFormation.describeStacks();
		Stack stack = stacksResult.getStacks().get(0);
		System.out.println("stack = " + stack);

		DescribeStackResourcesResult sample = this.amazonCloudFormation.describeStackResources(new DescribeStackResourcesRequest().withStackName("sample"));
		for (StackResource stackResource : sample.getStackResources()) {
			System.out.println("stackResource = " + stackResource);
		}
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testDeleteStack() throws Exception {
		this.amazonCloudFormation = new AmazonCloudFormationClient(new BasicAWSCredentials(this.accessKey, this.secretKey));
		this.amazonCloudFormation.setEndpoint("https://cloudformation.us-east-1.amazonaws.com");

		this.amazonCloudFormation.deleteStack(new DeleteStackRequest().withStackName("sample"));

	}
}
