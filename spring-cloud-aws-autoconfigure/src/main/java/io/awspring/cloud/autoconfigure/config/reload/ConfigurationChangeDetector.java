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
package io.awspring.cloud.autoconfigure.config.reload;

import io.awspring.cloud.core.config.AwsPropertySource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.log.LogAccessor;

/**
 * This is the superclass of all beans that can listen to changes in the configuration and fire a reload.
 *
 * Heavily inspired by Spring Cloud Kubernetes.
 *
 * @author Nicola Ferraro
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public abstract class ConfigurationChangeDetector<T extends AwsPropertySource<?, ?>> {

	private static final LogAccessor LOG = new LogAccessor(LogFactory.getLog(ConfigurationChangeDetector.class));

	protected ReloadProperties properties;

	protected ConfigurationUpdateStrategy strategy;

	protected ConfigurableEnvironment environment;

	private final Class<T> propertySourceClass;

	public ConfigurationChangeDetector(ReloadProperties properties, ConfigurationUpdateStrategy strategy,
			ConfigurableEnvironment environment, Class<T> propertySourceClass) {
		this.properties = Objects.requireNonNull(properties);
		this.strategy = Objects.requireNonNull(strategy);
		this.environment = environment;
		this.propertySourceClass = propertySourceClass;
	}

	public void reloadProperties() {
		LOG.info(() -> "Reloading using strategy: " + this.strategy);
		strategy.run();
	}

	/**
	 * Determines if two property sources are different.
	 * @param left left map property sources
	 * @param right right map property sources
	 * @return {@code true} if source has changed
	 */
	protected boolean changed(EnumerablePropertySource<?> left, EnumerablePropertySource<?> right) {
		if (left == right) {
			return false;
		}

		// check if a new property is added
		if (!Arrays.equals(left.getPropertyNames(), right.getPropertyNames())) {
			return true;
		}

		// check if a value of existing property changed
		for (String property : left.getPropertyNames()) {
			if (!Objects.equals(left.getProperty(property), right.getProperty(property))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a list of property sources that are instance of {@link T}.
	 *
	 * @param environment Spring environment
	 * @return a list of property sources
	 */
	protected List<T> locateMapPropertySources(ConfigurableEnvironment environment) {

		return environment.getPropertySources().stream()
				.filter(it -> (it.getClass().isAssignableFrom(propertySourceClass))).map(it -> (T) it)
				.collect(Collectors.toList());
	}

	public Class<T> getPropertySourceClass() {
		return propertySourceClass;
	}
}
