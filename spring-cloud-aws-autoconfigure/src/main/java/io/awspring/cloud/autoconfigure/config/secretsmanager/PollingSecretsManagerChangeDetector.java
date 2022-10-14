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

import io.awspring.cloud.autoconfigure.config.reload.ConfigurationChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.secretsmanager.SecretsManagerPropertySource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

public class PollingSecretsManagerChangeDetector extends ConfigurationChangeDetector {

	protected Log log = LogFactory.getLog(getClass());
	private final TaskScheduler taskExecutor;

	public PollingSecretsManagerChangeDetector(ReloadableProperties properties, ConfigurationUpdateStrategy strategy,
			TaskScheduler taskExecutor, ConfigurableEnvironment environment) {
		super(properties, strategy, environment);
		this.taskExecutor = taskExecutor;

	}

	@PostConstruct
	private void init() {
		log.info("Polling configurations change detector activated");
		long period = properties.getReload().getPeriod().toMillis();
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setInitialDelay(period);
		taskExecutor.schedule(this::executeCycle, trigger);
	}

	public void executeCycle() {
		if (this.properties.isMonitored()) {
			if (log.isDebugEnabled()) {
				log.debug("Polling for changes in secrets");
			}
			List<SecretsManagerPropertySource> currentSecretSources = locateMapPropertySources(this.environment);
			if (!currentSecretSources.isEmpty()) {
				for (SecretsManagerPropertySource propertySource : currentSecretSources) {
					SecretsManagerPropertySource clone = propertySource.copy();
					clone.init();
					if (changed(propertySource, clone)) {
						reloadProperties();
					}
				}
			}
		}
	}

	/**
	 * Determines if two property sources are different.
	 * @param left left map property sources
	 * @param right right map property sources
	 * @return {@code true} if source has changed
	 */
	public boolean changed(EnumerablePropertySource<?> left, EnumerablePropertySource<?> right) {
		if (left == right) {
			return false;
		}
		for (String property : left.getPropertyNames()) {
			if (!Objects.equals(left.getProperty(property), right.getProperty(property))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a list of MapPropertySource that correspond to the current state of the system. This only handles the
	 * PropertySource objects that are returned.
	 * @param environment Spring environment
	 * @return a list of MapPropertySource that correspond to the current state of the system
	 */
	protected List<SecretsManagerPropertySource> locateMapPropertySources(ConfigurableEnvironment environment) {

		return environment.getPropertySources().stream().filter(it -> (it instanceof SecretsManagerPropertySource))
				.map(it -> ((SecretsManagerPropertySource) it)).collect(Collectors.toList());
	}
}
