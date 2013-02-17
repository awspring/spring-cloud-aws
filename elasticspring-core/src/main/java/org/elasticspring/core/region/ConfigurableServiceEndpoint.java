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

package org.elasticspring.core.region;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurable {@link ServiceEndpoint} implementation, using the most common defaults for the Amazon webservices.
 * Sub-classes may use this implementation if the follow the standard rules for location and endpoint naming. Provides
 * configuration facilities through the constructor for the location name.
 * <p/>
 * <b>Note:</b> If there are big differences in the ServiceEndpoint implementation, consider using an own
 * implementation instead uf re-using this class (e.g. Amazon S3 has totally different naming schemes).
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class ConfigurableServiceEndpoint implements ServiceEndpoint {

	private static final String DEFAULT_SEPARATOR = ".";
	private static final String SERVICE_HOST = "amazonaws.com";
	private static final Map<Region, String> DEFAULT_REGION_LOCATION_MAPPINGS = getDefaultRegionLocationMappings();
	private final Region region;
	private final String serviceName;
	private final String location;


	/**
	 * Configure the class by using the Region and service name. This constructor uses the "default" location mappings
	 * which are used by most of the services.
	 *
	 * @param region
	 * 		- the {@link Region} to be used. Must not be null
	 * @param serviceName
	 * 		- the service name to be used (e.g. rds), must not be null
	 */
	protected ConfigurableServiceEndpoint(Region region, String serviceName) {
		this(region, serviceName, DEFAULT_REGION_LOCATION_MAPPINGS.get(region));
	}

	/**
	 * Constructs the object and additionally to {@link #ConfigurableServiceEndpoint(Region, String)} this constructor
	 * provides the possibility ot override the location name.
	 *
	 * @param region
	 * 		- the {@link Region} to be used. Must not be null
	 * @param serviceName
	 * 		- the service name to be used (e.g. rds), must not be null
	 * @param location
	 * 		- the location to be used (e.g. us-west-1), must not be null
	 */
	protected ConfigurableServiceEndpoint(Region region, String serviceName, String location) {
		Assert.notNull(region, "Region must not be null");
		Assert.notNull(serviceName, "ServiceName must not be null");
		Assert.notNull(location, "Location must not be null");
		this.region = region;
		this.serviceName = serviceName;
		this.location = location;
	}

	@Override
	public final Region getRegion() {
		return this.region;
	}

	@Override
	public final String getEndpoint() {
		return this.serviceName + DEFAULT_SEPARATOR + this.location + DEFAULT_SEPARATOR + SERVICE_HOST;
	}

	@Override
	public String getLocation() {
		return this.location;
	}

	private static Map<Region, String> getDefaultRegionLocationMappings() {
		HashMap<Region, String> mapping = new HashMap<Region, String>();
		mapping.put(Region.US_STANDARD, "us-east-1");
		mapping.put(Region.NORTHERN_CALIFORNIA, "us-west-1");
		mapping.put(Region.OREGON, "us-west-2");
		mapping.put(Region.IRELAND, "eu-west-1");
		mapping.put(Region.SINGAPORE, "ap-southeast-1");
		mapping.put(Region.SYDNEY, "ap-southeast-2");
		mapping.put(Region.TOKYO, "ap-northeast-1");
		mapping.put(Region.SAO_PAULO, "sa-east-1");
		return mapping;
	}
}