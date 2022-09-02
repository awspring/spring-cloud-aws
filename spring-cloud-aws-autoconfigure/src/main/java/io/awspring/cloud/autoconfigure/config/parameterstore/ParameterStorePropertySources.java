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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import io.awspring.cloud.parameterstore.ParameterStorePropertySource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * @author Eddú Meléndez
 * @since 2.3
 */
public class ParameterStorePropertySources {

	private static Log LOG = LogFactory.getLog(ParameterStorePropertySources.class);

	/**
	 * Creates property source for given context.
	 * @param context property source context equivalent to the parameter name
	 * @param optional if creating context should fail with exception if parameter cannot be loaded
	 * @param client System Manager Management client
	 * @return a property source or null if parameter could not be loaded and optional is set to true
	 */
	@Nullable
	public ParameterStorePropertySource createPropertySource(String context, boolean optional, SsmClient client) {
		Assert.notNull(context, "context is required");
		Assert.notNull(client, "SsmClient is required");

		LOG.info("Loading property from AWS Parameter Store with name: " + context + ", optional: " + optional);
		try {
			ParameterStorePropertySource propertySource = new ParameterStorePropertySource(context, client);
			propertySource.init();
			return propertySource;
			// TODO: howto call close when /refresh
		}
		catch (Exception e) {
			if (!optional) {
				throw new AwsParameterPropertySourceNotFoundException(e);
			}
			else {
				LOG.warn("Unable to load AWS parameter from " + context + ". " + e.getMessage());
			}
		}
		return null;
	}

	static class AwsParameterPropertySourceNotFoundException extends RuntimeException {

		AwsParameterPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
