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

/**
 * Provider that can be used to retrieve the configured {@link Region}. A region can be
 * typically configured on application or component level and defines which Region will be
 * used to access the services. Implementation of this interfaces might be simple static
 * region configuration which is feasible for single location applications, or dynamic
 * regions which can fetched from the execution environment where the application is
 * currently running.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public interface RegionProvider {

	/**
	 * Returns the region which should be used to access the services. The possible return
	 * values are already defined in the {@link Region} enumeration.
	 * @return the region which might be statically configured or dynamically fetched
	 */
	Region getRegion();

}
