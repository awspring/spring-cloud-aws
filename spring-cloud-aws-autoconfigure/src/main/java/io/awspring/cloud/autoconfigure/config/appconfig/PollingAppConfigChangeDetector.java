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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.autoconfigure.config.reload.ConfigurationChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

public class PollingAppConfigChangeDetector extends ConfigurationChangeDetector {

	protected Log log = LogFactory.getLog(getClass());
	private final TaskScheduler taskExecutor;
	private final long period;

	public PollingAppConfigChangeDetector(ReloadProperties properties, ConfigurationUpdateStrategy strategy,
			TaskScheduler taskExecutor, long period) {
		super(properties, strategy);
		this.taskExecutor = taskExecutor;
		this.period = period;
	}

	@PostConstruct
	private void init() {
		log.info("AppConfigData polling configurations change detector activated");
		PeriodicTrigger trigger = new PeriodicTrigger(period);
		trigger.setInitialDelay(period);
		taskExecutor.schedule(this::checkForChanges, trigger);
	}

	public void checkForChanges() {
		if (AppConfigDataLoader.checkIfReloadIsToBeDone()) {
			reloadProperties();
		}
	}

}
