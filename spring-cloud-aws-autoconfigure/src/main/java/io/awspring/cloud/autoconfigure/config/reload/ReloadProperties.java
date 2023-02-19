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

import java.time.Duration;
import org.springframework.lang.Nullable;

/**
 * Configuration related to reloading properties.
 *
 * Heavily inspired by Spring Cloud Kubernetes.
 *
 * @author Nicola Ferraro
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public class ReloadProperties {

	/**
	 * Reload strategy to run when properties change.
	 */
	@Nullable
	private ReloadStrategy strategy;

	/**
	 * If {@link ReloadStrategy#RESTART_CONTEXT} is configured, maximum waiting time for server restart.
	 */
	private Duration maxWaitForRestart = Duration.ofSeconds(2);

	/**
	 * Refresh period for {@link PollingAwsPropertySourceChangeDetector}.
	 */
	private Duration period = Duration.ofMinutes(1);

	public boolean isEnabled() {
		return strategy != null;
	}

	@Nullable
	public ReloadStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(@Nullable ReloadStrategy strategy) {
		this.strategy = strategy;
	}

	public Duration getMaxWaitForRestart() {
		return maxWaitForRestart;
	}

	public void setMaxWaitForRestart(Duration maxWaitForRestart) {
		this.maxWaitForRestart = maxWaitForRestart;
	}

	public Duration getPeriod() {
		return period;
	}

	public void setPeriod(Duration period) {
		this.period = period;
	}
}
