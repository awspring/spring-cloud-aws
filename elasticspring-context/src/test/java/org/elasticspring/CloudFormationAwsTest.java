/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring;

import com.amazonaws.auth.PropertiesCredentials;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class CloudFormationAwsTest {

	private AmazonCloudFormation amazonCloudFormation;

	@Test
	@IfProfileValue(name= "test-groups", value = "aws-test")
	public void testGetConfiguration() throws Exception {
		File file = new ClassPathResource("access.properties").getFile();
		this.amazonCloudFormation = new AmazonCloudFormationClient(new PropertiesCredentials(file));
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
	@IfProfileValue(name= "test-groups", value = "aws-test")
	public void testDeleteStack() throws Exception {
		File file = new ClassPathResource("access.properties").getFile();
		this.amazonCloudFormation = new AmazonCloudFormationClient(new PropertiesCredentials(file));
		this.amazonCloudFormation.setEndpoint("https://cloudformation.us-east-1.amazonaws.com");

		this.amazonCloudFormation.deleteStack(new DeleteStackRequest().withStackName("sample"));

	}
}
