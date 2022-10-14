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

import java.util.List;

import javax.annotation.PostConstruct;

import io.awspring.cloud.autoconfigure.config.secretsmanager.ReloadableProperties;
import io.awspring.cloud.core.config.AwsPropertySource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Configuration change detector that checks for changed configuration on a scheduled basis.
 *
 * Heavily inspired by Spring Cloud Kubernetes.
 *
 * @param <T> - property source class to check
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public class PollingAwsPropertySourceChangeDetector<T extends AwsPropertySource<?, ?>> extends ConfigurationChangeDetector<T> {

	protected Log log = LogFactory.getLog(getClass());
	private final TaskScheduler taskExecutor;

	public PollingAwsPropertySourceChangeDetector(ReloadableProperties properties, Class<T> clazz,
		ConfigurationUpdateStrategy strategy,
		TaskScheduler taskExecutor, ConfigurableEnvironment environment) {
		super(properties, strategy, environment, clazz);
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
			List<T> currentSecretSources = locateMapPropertySources(this.environment);
			if (!currentSecretSources.isEmpty()) {
				for (T propertySource : currentSecretSources) {
					AwsPropertySource<?, ?> clone = propertySource.copy();
					clone.init();
					if (changed(propertySource, clone)) {
						reloadProperties();
					}
				}
			}
		}
	}
}
