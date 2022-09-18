/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.cloudmap;

/**
 * Mock Responses from Ec2 meta-data endpoint
 * @author Hari Ohm Prasath
 * @since 3.0
 */
public class CloudMapTestConstants {
	/*
	 * Mock response
	 */
	public static final String ECS_REPONSE_JSON = "{\n" + "  \"Cluster\": \"default\",\n"
			+ "  \"TaskARN\": \"arn:aws:ecs:us-east-2:012345678910:task/9781c248-0edd-4cdb-9a93-f63cb662a5d3\",\n"
			+ "  \"Family\": \"nginx\",\n" + "  \"Revision\": \"5\",\n" + "  \"DesiredStatus\": \"RUNNING\",\n"
			+ "  \"KnownStatus\": \"RUNNING\",\n" + "  \"Containers\": [\n" + "    {\n"
			+ "      \"DockerId\": \"731a0d6a3b4210e2448339bc7015aaa79bfe4fa256384f4102db86ef94cbbc4c\",\n"
			+ "      \"Name\": \"~internal~ecs~pause\",\n"
			+ "      \"DockerName\": \"ecs-nginx-5-internalecspause-acc699c0cbf2d6d11700\",\n"
			+ "      \"Image\": \"amazon/amazon-ecs-pause:0.1.0\",\n" + "      \"ImageID\": \"\",\n"
			+ "      \"Labels\": {\n" + "        \"com.amazonaws.ecs.cluster\": \"default\",\n"
			+ "        \"com.amazonaws.ecs.container-name\": \"~internal~ecs~pause\",\n"
			+ "        \"com.amazonaws.ecs.task-arn\": \"arn:aws:ecs:us-east-2:012345678910:task/9781c248-0edd-4cdb-9a93-f63cb662a5d3\",\n"
			+ "        \"com.amazonaws.ecs.task-definition-family\": \"nginx\",\n"
			+ "        \"com.amazonaws.ecs.task-definition-version\": \"5\"\n" + "      },\n"
			+ "      \"DesiredStatus\": \"RESOURCES_PROVISIONED\",\n"
			+ "      \"KnownStatus\": \"RESOURCES_PROVISIONED\",\n" + "      \"Limits\": {\n" + "        \"CPU\": 0,\n"
			+ "        \"Memory\": 0\n" + "      },\n" + "      \"CreatedAt\": \"2018-02-01T20:55:08.366329616Z\",\n"
			+ "      \"StartedAt\": \"2018-02-01T20:55:09.058354915Z\",\n" + "      \"Type\": \"CNI_PAUSE\",\n"
			+ "      \"Networks\": [\n" + "        {\n" + "          \"NetworkMode\": \"awsvpc\",\n"
			+ "          \"IPv4Addresses\": [\n" + "            \"10.0.2.106\"\n" + "          ],\n"
			+ "          \"IPv4SubnetCIDRBlock\": [\n" + "            \"10.0.2.106\"\n" + "          ]\n"
			+ "        }\n" + "      ]\n" + "    },\n" + "    {\n"
			+ "      \"DockerId\": \"43481a6ce4842eec8fe72fc28500c6b52edcc0917f105b83379f88cac1ff3946\",\n"
			+ "      \"Name\": \"nginx-curl\",\n"
			+ "      \"DockerName\": \"ecs-nginx-5-nginx-curl-ccccb9f49db0dfe0d901\",\n"
			+ "      \"Image\": \"nrdlngr/nginx-curl\",\n"
			+ "      \"ImageID\": \"sha256:2e00ae64383cfc865ba0a2ba37f61b50a120d2d9378559dcd458dc0de47bc165\",\n"
			+ "      \"Labels\": {\n" + "        \"com.amazonaws.ecs.cluster\": \"default\",\n"
			+ "        \"com.amazonaws.ecs.container-name\": \"nginx-curl\",\n"
			+ "        \"com.amazonaws.ecs.task-arn\": \"arn:aws:ecs:us-east-2:012345678910:task/9781c248-0edd-4cdb-9a93-f63cb662a5d3\",\n"
			+ "        \"com.amazonaws.ecs.task-definition-family\": \"nginx\",\n"
			+ "        \"com.amazonaws.ecs.task-definition-version\": \"5\"\n" + "      },\n"
			+ "      \"DesiredStatus\": \"RUNNING\",\n" + "      \"KnownStatus\": \"RUNNING\",\n"
			+ "      \"Limits\": {\n" + "        \"CPU\": 512,\n" + "        \"Memory\": 512\n" + "      },\n"
			+ "      \"CreatedAt\": \"2018-02-01T20:55:10.554941919Z\",\n"
			+ "      \"StartedAt\": \"2018-02-01T20:55:11.064236631Z\",\n" + "      \"Type\": \"NORMAL\",\n"
			+ "      \"Networks\": [\n" + "        {\n" + "          \"NetworkMode\": \"awsvpc\",\n"
			+ "          \"IPv4Addresses\": [\n" + "            \"10.0.2.106\"\n" + "          ]\n" + "        }\n"
			+ "      ]\n" + "    }\n" + "  ],\n" + "  \"PullStartedAt\": \"2018-02-01T20:55:09.372495529Z\",\n"
			+ "  \"PullStoppedAt\": \"2018-02-01T20:55:10.552018345Z\",\n" + "  \"AvailabilityZone\": \"us-east-2b\"\n"
			+ "}";
}
