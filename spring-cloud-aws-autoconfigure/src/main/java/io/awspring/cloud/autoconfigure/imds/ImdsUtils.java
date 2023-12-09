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
package io.awspring.cloud.autoconfigure.imds;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

/**
 * Utility object for working with EC2 instance metadata service (IDMS). Determines if the service and its data are
 * available and provides utility methods for loading a PropertySource. Instance metadata is generally available - with
 * exceptions - when an application is running within EC2, Elastic Beanstalk, ECS, EKS, etc. The presence of instance
 * metadata can be used as a general indicator of whether an application is running within the AWS cloud or on a local
 * environment, however there are exceptions to the general principal, such as when EC2 instance deliberately disables
 * IMDS. Non-EC2 compute environments such as Lambda or Fargate do not provide the IMDS service.
 *
 * Works with either IMDS v1 or v2.
 *
 * @author Ken Krueger
 * @since 3.1.0
 */
public class ImdsUtils {

	private static Logger logger = LoggerFactory.getLogger(ImdsUtils.class);

	private final Ec2MetadataClient client;

	private Boolean isCloudEnvironment;

	private final String prefix = "/latest/meta-data/";

	private final String[] keys = { "ami-id", "ami-launch-index", "ami-manifest-path", "hostname", "instance-action",
			"instance-id", "instance-life-cycle", "instance-type", "local-hostname", "local-ipv4", "mac", "profile",
			"public-hostname", "public-ipv4", "reservation-id", "security-groups", "ipv6", "kernel-id", "iam/info",
			"product-codes", "ramdisk-id", "reservation-id", "services/domain", "services/partition", "tags/instance",
			"autoscaling/target-lifecycle-state", "placement/availability-zone", "placement/availability-zone-id",
			"placement/group-name", "placement/host-id", "placement/partition-number", "placement/region"

	};

	public ImdsUtils(Ec2MetadataClient client) {
		this.client = client;
	}

	public boolean isRunningOnCloudEnvironment() {
		if (isCloudEnvironment == null) {
			isCloudEnvironment = false;
			try {
				Ec2MetadataResponse response = client.get("/latest/meta-data/ami-id");
				isCloudEnvironment = response.asString() != null && response.asString().length() > 0;
			}
			catch (SdkClientException e) {
				if (e.getMessage().contains("retries")) {
					// Ignore any exceptions about exceeding retries.
					// This is expected when instance metadata is not available.
				}
				else {
					logger.debug("Error occurred when accessing instance metadata.", e);
				}
			}
			catch (Exception e) {
				logger.error("Error occurred when accessing instance metadata.", e);
			}
			finally {
				if (isCloudEnvironment) {
					logger.info("EC2 Instance MetaData detected, application is running within an EC2 instance.");
				}
				else {
					logger.info(
							"EC2 Instance MetaData not detected, application is NOT running within an EC2 instance.");
				}
			}
		}
		return isCloudEnvironment;
	}

	/**
	 * Load EC2 Instance Metadata into a simple Map structure, suitable for use populating a PropertySource.
	 * @return Map of metadata properties. Empty Map if instance metadata is not available.
	 *
	 * @see ImdsPropertySource
	 * @see ImdsAutoConfiguration
	 */
	public Map<String, String> getEc2InstanceMetadata() {
		Map<String, String> properties = new LinkedHashMap<>();
		if (!isRunningOnCloudEnvironment())
			return properties;

		Arrays.stream(keys).forEach(t -> mapPut(properties, t));

		return properties;
	}

	/**
	 * Internal utility method for safely loading a candidate key into the given Map. Silently ignores various expected
	 * cases where keys are not present.
	 * @param map
	 * @param key
	 */
	private void mapPut(Map<String, String> map, String key) {
		try {
			Ec2MetadataResponse response = client.get(prefix + key);
			if (response != null) {
				map.put(key, response.asString());
			}
		}
		catch (SdkClientException e) {
			logger.debug("Unable to read property " + prefix + key + ", exception message: " + e.getMessage());
		}
		catch (RuntimeException e) {
			logger.debug(
					"Exception occurred reading property " + prefix + key + ", exception message: " + e.getMessage());
		}
	}

}
