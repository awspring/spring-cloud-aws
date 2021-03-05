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

package org.springframework.cloud.aws.cloudmap;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Uses Fargate Metadata URL to retrieve IPv4 address and VPC ID to register instances to
 * cloudmap.
 *
 * @author Hari Ohm Prasath
 * @since 1.0
 */
public class CloudMapUtils {

	private final RestTemplate REST_TEMPLATE = new RestTemplate();

	private final Logger log = LoggerFactory.getLogger(CloudMapUtils.class);

	private final ObjectMapper JSON_MAPPER = new ObjectMapper();

	static final String IPV_4_ADDRESS = "IPV4_ADDRESS";
	static final String VPC_ID = "VPC_ID";

	private AmazonEC2 ec2Client;

	/**
	 * Uses ECS Fargate metadata URL to fetch all the required details around IP address
	 * and VpcID to register instances to cloudmap service.
	 * @return map containing ip address and vpcid
	 */
	Map<String, String> getRegistrationAttributes() {
		Map<String, String> attributes = new HashMap<>();
		try {
			ResponseEntity<String> metaDataResponse = REST_TEMPLATE
					.getForEntity(System.getenv("ECS_CONTAINER_METADATA_URI_V4") + "/task", String.class);
			JsonNode root = JSON_MAPPER.readTree(metaDataResponse.getBody());
			JsonNode jsonNode = root.get("Containers").get(0).get("Networks").get(0);
			final String ipv4Address = jsonNode.get("IPv4Addresses").get(0).asText();
			final String cidrBlock = jsonNode.get("IPv4SubnetCIDRBlock").asText();
			final String vpcId = getEc2Client()
					.describeSubnets(new DescribeSubnetsRequest()
							.withFilters(new Filter().withName("cidr-block").withValues(cidrBlock)))
					.getSubnets().get(0).getVpcId();
			log.info("IPv4Address {} - CIDR Block {} - VPC ID {}", ipv4Address, cidrBlock, vpcId);

			attributes.put(IPV_4_ADDRESS, ipv4Address);
			attributes.put(VPC_ID, vpcId);
		}
		catch (Exception e) {
			log.error("Error while fetching network details - {}", e.getMessage(), e);
		}

		return attributes;
	}

	AmazonEC2 getEc2Client() {
		if (ec2Client == null) {
			ec2Client = AmazonEC2ClientBuilder.standard().withRegion(System.getenv("AWS_REGION"))
					.withCredentials(new DefaultAWSCredentialsProviderChain()).build();
		}

		return ec2Client;
	}

}
