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

package org.springframework.cloud.aws.core.region;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;

/**
 * Static {@link RegionProvider} implementation that can used to statically configure a
 * region. The region could be provided through a configuration file at configuration
 * time.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class StaticRegionProvider implements RegionProvider {

	private final Region configuredRegion;

	/**
	 * Constructs and configures the static region for this RegionProvider implementation.
	 * @param configuredRegion - the region that will be statically returned in
	 * {@link #getRegion()}
	 */
	@RuntimeUse
	public StaticRegionProvider(String configuredRegion) {
		try {
			this.configuredRegion = Region.getRegion(Regions.fromName(configuredRegion));
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"The region '" + configuredRegion + "' is not a valid region!", e);
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
