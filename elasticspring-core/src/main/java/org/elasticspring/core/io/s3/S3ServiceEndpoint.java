/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3;

import org.elasticspring.core.region.Region;
import org.elasticspring.core.region.ServiceEndpoint;

/**
 * Enum that holds all available regions for the S3 service.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public enum S3ServiceEndpoint implements ServiceEndpoint {

	US_STANDARD(Region.US_STANDARD, "US", "s3.amazonaws.com"),
	NORTHERN_CALIFORNIA(Region.NORTHERN_CALIFORNIA, "us-west-1", "s3-us-west-1.amazonaws.com"),
	OREGON(Region.OREGON, "us-west-2", "s3-us-west-2.amazonaws.com"),
	IRELAND(Region.IRELAND, "EU", "s3-eu-west-1.amazonaws.com"),
	SINGAPORE(Region.SINGAPORE, "ap-southeast-1", "s3-ap-southeast-1.amazonaws.com"),
	SYDNEY(Region.SYDNEY, "ap-southeast-2", "s3-ap-southeast-2.amazonaws.com"),
	TOKYO(Region.TOKYO, "ap-northeast-1", "s3-ap-northeast-1.amazonaws.com"),
	SAO_PAULO(Region.SAO_PAULO, "sa-east-1", "s3-sa-east-1.amazonaws.com");

	private final Region region;
	private final String location;
	private final String endpoint;

	S3ServiceEndpoint(Region region, String location, String endpoint) {
		this.region = region;
		this.location = location;
		this.endpoint = endpoint;
	}

	@Override
	public Region getRegion() {
		return this.region;
	}

	@Override
	public String getEndpoint() {
		return this.endpoint;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	public static S3ServiceEndpoint fromLocation(String location) {
		for (S3ServiceEndpoint s3Region : values()) {
			if (s3Region.getLocation().equalsIgnoreCase(location)) {
				return s3Region;
			}
		}
		throw new IllegalArgumentException("No S3ServiceEndpoint found for location:'" + location + "'");
	}

	public static S3ServiceEndpoint fromRegion(Region region) {
		for (S3ServiceEndpoint s3Region : values()) {
			if (s3Region.getRegion() == region) {
				return s3Region;
			}
		}
		throw new IllegalArgumentException("No S3ServiceEndpoint found for region:'" + (region != null ? region.name() : "null") + "'");
	}
}