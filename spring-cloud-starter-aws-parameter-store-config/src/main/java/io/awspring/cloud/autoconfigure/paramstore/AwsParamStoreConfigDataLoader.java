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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import io.awspring.cloud.paramstore.AwsParamStorePropertySource;
import io.awspring.cloud.paramstore.AwsParamStorePropertySources;
import org.apache.commons.logging.Log;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.util.ReflectionUtils;

/**
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class AwsParamStoreConfigDataLoader implements ConfigDataLoader<AwsParamStoreConfigDataResource> {

	private final DeferredLogFactory logFactory;

	public AwsParamStoreConfigDataLoader(DeferredLogFactory logFactory) {
		this.logFactory = logFactory;
		reconfigureLoggers(logFactory);
	}

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

	private void reconfigureLoggers(DeferredLogFactory logFactory) {
		// loggers in these classes must be static non-final
		List<Class<?>> loggers = Arrays.asList(AwsParamStorePropertySource.class, AwsParamStorePropertySources.class);

		loggers.forEach(it -> reconfigureLogger(it, logFactory));
	}

	static void reconfigureLogger(Class<?> type, DeferredLogFactory logFactory) {
		ReflectionUtils.doWithFields(type, field -> {

			field.setAccessible(true);
			field.set(null, logFactory.getLog(type));

		}, AwsParamStoreConfigDataLoader::isUpdateableLogField);
	}

	private static boolean isUpdateableLogField(Field field) {
		return !Modifier.isFinal(field.getModifiers()) && field.getType().isAssignableFrom(Log.class);
	}

}
