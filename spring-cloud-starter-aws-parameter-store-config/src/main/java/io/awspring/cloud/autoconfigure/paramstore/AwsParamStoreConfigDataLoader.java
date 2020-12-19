/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.autoconfigure.paramstore;

import java.util.Collections;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import io.awspring.cloud.paramstore.AwsParamStorePropertySource;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class AwsParamStoreConfigDataLoader implements ConfigDataLoader<AwsParamStoreConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, AwsParamStoreConfigDataResource resource) {
		try {
			AWSSimpleSystemsManagement ssm = context.getBootstrapContext().get(AWSSimpleSystemsManagement.class);
			AwsParamStorePropertySource propertySource = resource.getPropertySources()
					.createPropertySource(resource.getContext(), resource.isOptional(), ssm);
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
