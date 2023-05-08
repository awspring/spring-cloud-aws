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
package io.awspring.cloud.core.region;

import org.springframework.util.Assert;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * Static {@link AwsRegionProvider} implementation that can used to statically configure a region. The region could be
 * provided through a configuration file at configuration time.
 *
 * @author Agim Emruli
 * @author Maciej Walkowiak
 * @since 1.0
 */
public class StaticRegionProvider implements AwsRegionProvider {

	private final Region configuredRegion;

	/**
	 * Constructs and configures the static region for this RegionProvider implementation.
	 * @param configuredRegion - the region that will be statically returned in {@link #getRegion()}
	 */
	public StaticRegionProvider(String configuredRegion) {
		Assert.notNull(configuredRegion, "region is required");

		try {
			this.configuredRegion = Region.of(configuredRegion);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The region '" + configuredRegion + "' is not a valid region!", e);
		}
	}

	/**
	 * Return the configured Region configured at construction time.
	 * @return the configured region, for every call the same
	 */
	@Override
	public Region getRegion() {
		return this.configuredRegion;
	}

}
