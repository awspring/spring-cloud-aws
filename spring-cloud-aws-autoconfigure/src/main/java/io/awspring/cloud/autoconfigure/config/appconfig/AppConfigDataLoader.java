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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.autoconfigure.config.BootstrapLoggingHelper;
import java.util.Collections;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/**
 * {@link ConfigDataLoader} for AWS AppConfig.
 *
 * @author Matej Nedic
 * @since 3.0
 */
public class AppConfigDataLoader implements ConfigDataLoader<AppConfigDataResource> {

	public AppConfigDataLoader(DeferredLogFactory logFactory) {
		BootstrapLoggingHelper.reconfigureLoggers(logFactory, "io.awspring.cloud.appconfig.AppConfigPropertySource",
				"io.awspring.cloud.autoconfigure.config.appconfig.AppConfigPropertySources");
	}

	@Override
	@Nullable
	public ConfigData load(ConfigDataLoaderContext context, AppConfigDataResource resource) {

		try {
			AppConfigDataClient appConfigDataClient = context.getBootstrapContext().get(AppConfigDataClient.class);
			AppConfigPropertySource propertySource = resource.getPropertySources()
					.createPropertySource(resource.getContext(), resource.isOptional(), appConfigDataClient);
			if (propertySource != null) {
				return new ConfigData(Collections.singletonList(propertySource));
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new ConfigDataResourceNotFoundException(resource, e);
		}
	}

}
