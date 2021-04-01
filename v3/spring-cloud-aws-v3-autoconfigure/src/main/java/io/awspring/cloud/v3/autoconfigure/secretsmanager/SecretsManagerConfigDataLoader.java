/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.autoconfigure.secretsmanager;

import java.util.Collections;

import io.awspring.cloud.v3.secretsmanager.SecretsManagerPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;

/**
 * Loads config data from AWS Secret Manager.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @since 2.3.0
 */
public class SecretsManagerConfigDataLoader implements ConfigDataLoader<SecretsManagerConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, SecretsManagerConfigDataResource resource) {
		try {
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
		catch (Exception e) {
			throw new ConfigDataResourceNotFoundException(resource, e);
		}
	}

}
