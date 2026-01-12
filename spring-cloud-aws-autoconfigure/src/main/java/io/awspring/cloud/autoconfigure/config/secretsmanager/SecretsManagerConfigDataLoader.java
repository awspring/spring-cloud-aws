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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

import io.awspring.cloud.autoconfigure.config.BootstrapLoggingHelper;
import io.awspring.cloud.secretsmanager.SecretsManagerPropertySource;
import java.util.Collections;
import java.util.Map;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Loads config data from AWS Secret Manager.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @since 2.3.0
 */
public class SecretsManagerConfigDataLoader implements ConfigDataLoader<SecretsManagerConfigDataResource> {

	public SecretsManagerConfigDataLoader(DeferredLogFactory logFactory) {
		BootstrapLoggingHelper.reconfigureLoggers(logFactory,
				"io.awspring.cloud.secretsmanager.SecretsManagerPropertySource",
				"io.awspring.cloud.autoconfigure.config.secretsmanager.SecretsManagerPropertySources");
	}

	@Override
	@Nullable
	public ConfigData load(ConfigDataLoaderContext context, SecretsManagerConfigDataResource resource) {
		// resource is disabled if secrets manager integration is disabled via
		// spring.cloud.aws.secretsmanager.enabled=false
		if (resource.isEnabled()) {
			SecretsManagerClient sm = context.getBootstrapContext().get(SecretsManagerClient.class);
			SecretsManagerPropertySource propertySource = resource.getPropertySources()
					.createPropertySource(resource.getContext(), resource.isOptional(), sm);
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
					Collections.singletonList(new MapPropertySource("aws-secretsmanager:" + context, Map.of())));
		}
	}

}
