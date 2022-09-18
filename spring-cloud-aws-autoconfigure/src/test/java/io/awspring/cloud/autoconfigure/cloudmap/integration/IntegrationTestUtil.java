/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.autoconfigure.cloudmap.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.localstack.LocalStackContainer;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SSM;

/**
 * Utility class for integration tests.
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class IntegrationTestUtil {
	public static final String REGION = "us-east-1";
	public static final String CIDR_BLOCK = "10.0.0.0/16";
	public static final String META_DATA_MOCK_RESPONSE = "https://5qjz8e33hj.api.quickmocker.com";
	public static final String LOCAL_STACK_API = "LOCALSTACK_API_KEY";
	public static final String LOCAL_STACK_API_KEY = ""; // TODO: Add your localstack api key here

	public static final String TEST_DEFAULT_NAMESPACE = "test.namespace";
	public static final String TEST_DEFAULT_SERVICE = "test.service";

	public static String createVpc(LocalStackContainer localStackContainer) {
		try {
			final Container.ExecResult execResult = localStackContainer.execInContainer("awslocal", "ec2", "create-vpc",
				"--cidr-block", CIDR_BLOCK, "--region", REGION);
			if (execResult.getExitCode() != 0) {
				throw new RuntimeException("Failed to create VPC: " + execResult.getStderr());
			}
			else {
				return execResult.getStdout().split("VpcId")[1].split("\"")[2];
			}
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void createSubnet(LocalStackContainer localStackContainer, String vpcId) {
		try {
			localStackContainer.execInContainer("awslocal", "ec2", "create-subnet", "--vpc-id", vpcId,
				"--cidr-block", CIDR_BLOCK, "--region", REGION);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void createCloudMapResources(LocalStackContainer localStackContainer, String namespace,
		String service, String vpcId) {
		try {
			// Create namespace
			localStackContainer.execInContainer("awslocal", "servicediscovery",
					"create-private-dns-namespace", "--name", namespace, "--region", REGION, "--vpc", vpcId);

			// List namespaces and get the id
			final Container.ExecResult listNameSpaceOutput = localStackContainer.execInContainer("awslocal", "servicediscovery",
					"list-namespaces", "--region", REGION);
			final String namespaceId = listNameSpaceOutput.getStdout().split("Id")[1].split("\"")[2];

			// Create service
			final String serviceId = localStackContainer.execInContainer("awslocal", "servicediscovery",
					"create-service", "--name", service, "--namespace-id", namespaceId, "--region", REGION)
				.getStdout().split("Id")[1].split("\"")[2];
			localStackContainer.execInContainer("awslocal", "servicediscovery", "register-instance",
				"--service-id", serviceId, "--attributes", "IPV4_ADDRESS=10.0.0.1,VPC_ID=" + vpcId, "--region", REGION);
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getCloudMapRegisteredInstances(LocalStackContainer localStackContainer, String serviceId) {
		try {
			final Container.ExecResult execResult = localStackContainer.execInContainer("awslocal", "servicediscovery",
				"list-instances", "--service-id", serviceId, "--region", REGION);
			if (execResult.getExitCode() != 0) {
				throw new RuntimeException("Failed to list instances: " + execResult.getStderr());
			}
			else {
				return execResult.getStdout().split("InstanceId").length;
			}
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> getCloudMapSpringBootProperties(LocalStackContainer localStackContainer) {
		List<String> properties = new ArrayList<>();
		properties.add("--spring.cloud.aws.cloudmap.region=" + REGION);
		properties.add("--spring.cloud.aws.cloudmap=http://non-existing-host/");
		properties.add("--spring.cloud.aws.cloudmap.endpoint=" + localStackContainer.getEndpointOverride(SSM)
			.toString());
		properties.add("--spring.cloud.aws.credentials.access-key=noop");
		properties.add("--spring.cloud.aws.credentials.secret-key=noop");
		properties.add("--spring.cloud.aws.region.static=" + REGION);
		properties.add("--spring.cloud.aws.cloudmap.enabled=true");

		return properties;
	}
}
