/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.appconfig.RequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/**
 * Provides prefix config import support.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AppConfigPropertySources {
	private static final Log LOG = LogFactory.getLog(AppConfigPropertySources.class);

	/**
	 * Creates property source for given context.
	 * @param context property source context extracted from the parameter name
	 * @param optional if creating context should fail with exception if parameter cannot be loaded
	 * @param client AWS AppConfigData client
	 * @return a property source or null if config could not be loaded and optional is set to true
	 */
	@Nullable
	public AppConfigPropertySource createPropertySource(RequestContext context, boolean optional,
			AppConfigDataClient client) {
		Assert.notNull(context, "RequestContext is required");
		Assert.notNull(client, "AppConfigDataClient is required");

		LOG.info("Loading properties from AWS App Config: " + context + ", optional: " + optional);
		try {
			AppConfigPropertySource propertySource = new AppConfigPropertySource(context, client);
			propertySource.init();
			return propertySource;
		}
		catch (Exception e) {
			LOG.warn("Unable to load AWS App Config from " + context + ". " + e.getMessage());
			if (!optional) {
				throw new AwsAppConfigPropertySourceNotFoundException(e);
			}
		}
		return null;
	}
}
