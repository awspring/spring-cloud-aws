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
import io.awspring.cloud.autoconfigure.config.BootstrapLoggingHelper;
import java.util.Collections;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;

/**
 * Loads config data from AWS AppConfig.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AppConfigConfigDataLoader implements ConfigDataLoader<AppConfigDataResource> {

	public AppConfigConfigDataLoader(DeferredLogFactory logFactory) {
		BootstrapLoggingHelper.reconfigureLoggers(logFactory, "io.awspring.cloud.appconfig.AppConfigPropertySource",
				"io.awspring.cloud.autoconfigure.config.appconfig.AppConfigPropertySources");
	}

	@Override
	@Nullable
	public ConfigData load(ConfigDataLoaderContext context, AppConfigDataResource resource) {
		// resource is disabled if appconfig integration is disabled via
		// spring.cloud.aws.appconfig.enabled=false
		if (resource.isEnabled()) {
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
		else {
			// create dummy empty config data
			return new ConfigData(
					Collections.singletonList(new MapPropertySource("aws-appconfig:" + context, Map.of())));
		}
	}
}
