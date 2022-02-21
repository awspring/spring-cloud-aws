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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.awspring.cloud.v3.secretsmanager.SecretsManagerPropertySource;
import org.apache.commons.logging.Log;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Loads config data from AWS Secret Manager.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @since 2.3.0
 */
public class SecretsManagerConfigDataLoader implements ConfigDataLoader<SecretsManagerConfigDataResource> {

	private final DeferredLogFactory logFactory;

	public SecretsManagerConfigDataLoader(DeferredLogFactory logFactory) {
		this.logFactory = logFactory;
		reconfigureLoggers(logFactory);
	}

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

	private void reconfigureLoggers(DeferredLogFactory logFactory) {
		// loggers in these classes must be static non-final
		List<Class<?>> loggers = new ArrayList<>();
		loggers.add(SecretsManagerPropertySources.class);
		// class may be not present if secrets manager module is not on the classpath
		if (ClassUtils.isPresent("io.awspring.cloud.v3.secretsmanager.SecretsManagerPropertySource", null)) {
			loggers.add(SecretsManagerPropertySource.class);
		}

		loggers.forEach(it -> reconfigureLogger(it, logFactory));
	}

	static void reconfigureLogger(Class<?> type, DeferredLogFactory logFactory) {
		ReflectionUtils.doWithFields(type, field -> {

			field.setAccessible(true);
			field.set(null, logFactory.getLog(type));

		}, SecretsManagerConfigDataLoader::isUpdateableLogField);
	}

	private static boolean isUpdateableLogField(Field field) {
		return !Modifier.isFinal(field.getModifiers()) && field.getType().isAssignableFrom(Log.class);
	}

}
