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

package org.elasticspring.jdbc.rds.config.endpoint;

import org.elasticspring.core.region.ConfigurableServiceEndpoint;
import org.elasticspring.core.region.Region;
import org.elasticspring.core.support.documentation.RuntimeUse;

/**
 * @author Agim Emruli
 * @since 1.0
 */
@RuntimeUse
public class AmazonRdsServiceEndpoint extends ConfigurableServiceEndpoint {

	private static final String SERVICE_NAME = "rds";

	public AmazonRdsServiceEndpoint(Region region) {
		super(region, SERVICE_NAME);
	}
}