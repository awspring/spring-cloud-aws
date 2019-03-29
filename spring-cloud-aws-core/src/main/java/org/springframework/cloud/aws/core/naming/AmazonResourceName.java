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

package org.springframework.cloud.aws.core.naming;

import java.util.Arrays;

import com.amazonaws.regions.Region;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Support class which is used to parse Amazon Webservice Resource Names. Resource names
 * are unique identifiers for different type of resource inside the Amazon Webservices.
 * These resource are represented by a service and could be bound into a particular
 * region, if the service is region based. Also most of the resources will be typically
 * bound to an account if the resource is scoped ot an account.
 * <p>
 * More information on resources are available on <a href=
 * "https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html">the Amazon
 * Webservice Manual</a>
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public final class AmazonResourceName {

	private AmazonResourceName() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	/*
	 * The delimiter for the arn which delimits the different parts of the arn string
	 */
	private static final String RESOURCE_NAME_DELIMITER = ":";

	/*
	 * The resource type delimiter which is used on particular resources to split the name
	 * and type. This typically the case for service which can contain nested paths (e.g.
	 * S3 Buckets, IAM groups)
	 */
	private static final String RESOURCE_TYPE_DELIMITER = "/";

	private final String partition;

	private final String service;

	private final String region;

	private final String account;

	private final String resourceType;

	private final String resourceName;

	private final String actualResourceTypeDelimiter;

	private AmazonResourceName(String partition, String service, String region,
			String account, String resourceType, String resourceName,
			String actualResourceTypeDelimiter) {
		Assert.notNull(partition, "partition must not be null");
		Assert.notNull(service, "service must not be null");
		Assert.notNull(resourceType, "resourceType must not be null");
		this.partition = partition;
		this.service = service;
		this.region = region;
		this.account = account;
		this.resourceType = resourceType;
		this.resourceName = resourceName;
		this.actualResourceTypeDelimiter = actualResourceTypeDelimiter;
	}

	public static AmazonResourceName fromString(String name) {
		Assert.notNull(name, "name must not be null");
		String[] tokens = name.split(RESOURCE_NAME_DELIMITER);
		if (tokens.length < 6 || tokens.length > 7) {
			throw new IllegalArgumentException(
					"Resource name:'" + name + "' is not a valid resource identifier");
		}

		if (!"arn".equals(tokens[0])) {
			throw new IllegalArgumentException("Resource name:'" + name
					+ "' must have an arn qualifier at the beginning");
		}

		if (!("aws".equals(tokens[1]) || tokens[1].startsWith("aws-"))) {
			throw new IllegalArgumentException(
					"Resource name:'" + name + "' must have a valid partition name");
		}

		String actualResourceTypeDelimiter;
		if (tokens.length == 6) {
			tokens = Arrays.copyOf(tokens, 7);
			String[] split = StringUtils.split(tokens[5], RESOURCE_TYPE_DELIMITER);
			if (split != null) {
				tokens[5] = split[0];
				tokens[6] = split[1];
			}
			actualResourceTypeDelimiter = RESOURCE_TYPE_DELIMITER;
		}
		else {
			actualResourceTypeDelimiter = RESOURCE_NAME_DELIMITER;
		}

		return new AmazonResourceName(tokens[1], tokens[2], trimToNull(tokens[3]),
				trimToNull(tokens[4]), trimToNull(tokens[5]), trimToNull(tokens[6]),
				actualResourceTypeDelimiter);
	}

	public static boolean isValidAmazonResourceName(String name) {
		try {
			fromString(name);
			return true;
		}
		catch (IllegalArgumentException ignore) {
			return false;
		}
	}

	private static String trimToNull(String input) {
		return StringUtils.hasText(input) ? input : null;
	}

	/**
	 * Returns the partition name of the service (currently aws, aws-cn, aws-us-gov).
	 * @return - the partition name - must start with aws
	 */
	public String getPartition() {
		return this.partition;
	}

	/**
	 * Returns the service name for this particular AmazonResourceName. The service name
	 * is a plain string which will be one of the service from the namespace defined at
	 * <a href=
	 * "https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#genref-aws-service-namespaces">user
	 * manual</a>
	 * @return - the service as a string - never be null
	 */
	public String getService() {
		return this.service;
	}

	/**
	 * Returns the region to which the particular service for this object is bound. This
	 * is one of the known regions or null if the service is globally available (e.g.
	 * Amazon S3)
	 * @return - the region or null if the service is globally available
	 */
	public String getRegion() {
		return this.region;
	}

	/**
	 * Returns the account to which the resource is assigned. This is the account id to
	 * which the owner of the resources belongs to. If the resources does not have any
	 * owner (e.g. solutions stacks in ElasticBeansTalk) this method will return null
	 * @return - the account number of the resource owner or null for non assigned
	 * resources
	 */
	public String getAccount() {
		return this.account;
	}

	/**
	 * Return the resource type for the resource. This is service dependent and can be an
	 * user (for IAM) or a bucket (for S3). The resource type is never null and can be the
	 * resource name for particular service (e.g. Amazon SQS Queue or Amazon SNS Topic)
	 * @return the type of the resource of the name of the resource itself
	 */
	public String getResourceType() {
		return this.resourceType;
	}

	/**
	 * Return the name of the resource inside the particular type. This can be a bucket
	 * name for Amazon S3 or the subscriptions id for the subscription of one particular
	 * topic.
	 * @return - the resource name or null if the resource type itself is the name of the
	 * resource
	 */
	public String getResourceName() {
		return this.resourceName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("arn");
		builder.append(RESOURCE_NAME_DELIMITER);
		builder.append(this.partition);
		builder.append(RESOURCE_NAME_DELIMITER);
		builder.append(this.service);
		builder.append(RESOURCE_NAME_DELIMITER);
		if (this.region != null) {
			builder.append(this.region);
		}
		builder.append(RESOURCE_NAME_DELIMITER);
		if (this.account != null) {
			builder.append(this.account);
		}
		builder.append(RESOURCE_NAME_DELIMITER);
		builder.append(this.resourceType);
		if (this.resourceName != null) {
			builder.append(this.actualResourceTypeDelimiter);
			builder.append(this.resourceName);
		}
		return builder.toString();
	}

	/**
	 * Builder for Amazon resources.
	 */
	@SuppressWarnings("ClassNamingConvention")
	public static class Builder {

		private String partition = "aws";

		private String service;

		private String region;

		private String account;

		private String resourceType;

		private String resourceName;

		private String actualResourceTypeDelimiter;

		public Builder withPartition(String qualifier) {
			this.partition = qualifier;
			return this;
		}

		public Builder withService(String service) {
			this.service = service;
			return this;
		}

		public Builder withRegion(Region region) {
			this.region = region.getName();
			return this;
		}

		public Builder withAccount(String account) {
			this.account = account;
			return this;
		}

		public Builder withResourceType(String resourceType) {
			this.resourceType = resourceType;
			return this;
		}

		public Builder withResourceName(String resourceName) {
			this.resourceName = resourceName;
			return this;
		}

		public Builder withResourceTypeDelimiter(String resourceTypeDelimiter) {
			this.actualResourceTypeDelimiter = resourceTypeDelimiter;
			return this;
		}

		public AmazonResourceName build() {
			return new AmazonResourceName(this.partition, this.service, this.region,
					this.account, this.resourceType, this.resourceName,
					this.actualResourceTypeDelimiter);
		}

	}

}
