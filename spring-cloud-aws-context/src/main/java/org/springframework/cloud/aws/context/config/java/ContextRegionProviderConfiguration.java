/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.context.config.java;

import com.amazonaws.regions.Regions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * @author Agim Emruli
 */
@Configuration
public class ContextRegionProviderConfiguration {

	@Autowired
	private Environment environment;

	//TODO: Check how to re-use the constant in AmazonWebserviceClientConfigurationUtils
	@Bean(name = "org.springframework.cloud.aws.core.region.RegionProvider.BEAN_NAME")
	@ConditionalOnProperty("cloud.aws.region.auto")
	public RegionProvider autoDetectingRegionProvider() {
		return new Ec2MetadataRegionProvider();
	}

	@Bean(name = "org.springframework.cloud.aws.core.region.RegionProvider.BEAN_NAME")
	@ConditionalOnProperty("cloud.aws.region.static")
	public RegionProvider staticRegionProvider() {
		String regionName = this.environment.getProperty("cloud.aws.region.static");
		Regions region;
		try {
			region = Regions.valueOf(regionName);
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException("Error parsing region with name:'" + regionName +
					"' valid values are:'" + Arrays.toString(Regions.values()) + "'",iae);
		}

		return new StaticRegionProvider(region);
	}
}
