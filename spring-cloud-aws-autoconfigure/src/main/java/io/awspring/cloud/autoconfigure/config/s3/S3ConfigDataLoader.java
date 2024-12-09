/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.config.s3;

import io.awspring.cloud.autoconfigure.config.BootstrapLoggingHelper;
import io.awspring.cloud.s3.config.S3PropertySource;
import java.util.Collections;
import java.util.Map;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Loads config data from AWS S3.
 *
 * @author Kunal Varpe
 * @since 3.3.0
 */
public class S3ConfigDataLoader implements ConfigDataLoader<S3ConfigDataResource> {

	public S3ConfigDataLoader(DeferredLogFactory logFactory) {
		BootstrapLoggingHelper.reconfigureLoggers(logFactory, "io.awspring.cloud.s3.config.S3PropertySource",
				"io.awspring.cloud.autoconfigure.config.s3.S3PropertySources");
	}

	@Override
	@Nullable
	public ConfigData load(ConfigDataLoaderContext context, S3ConfigDataResource resource) {
		try {
			// resource is disabled if s3 integration is disabled via
			// spring.cloud.aws.s3.config.enabled=false
			if (resource.isEnabled()) {
				S3Client s3Client = context.getBootstrapContext().get(S3Client.class);
				S3PropertySource propertySource = resource.getPropertySources()
						.createPropertySource(resource.getContext(), resource.isOptional(), s3Client);
				if (propertySource != null) {
					return new ConfigData(Collections.singletonList(propertySource));
				}
				else {
					return null;
				}
			}
			else {
				// create dummy empty config data
				return new ConfigData(Collections.singletonList(new MapPropertySource("aws-s3:" + context, Map.of())));
			}
		}
		catch (Exception e) {
			throw new ConfigDataResourceNotFoundException(resource, e);
		}

	}

}
