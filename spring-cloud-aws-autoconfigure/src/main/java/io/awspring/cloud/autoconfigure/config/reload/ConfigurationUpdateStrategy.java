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

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.util.Assert;

/**
 * This is the superclass of all named strategies that can be fired when the configuration changes.
 *
 * Heavily inspired by Spring Cloud Kubernetes.
 *
 * @author Nicola Ferraro
 * @author Maciej Walkowiak
 */
public class ConfigurationUpdateStrategy implements Runnable {

	private final Runnable reloadProcedure;

	public static ConfigurationUpdateStrategy create(ReloadProperties reloadProperties, ContextRefresher refresher,
			Optional<RestartEndpoint> restarter) {
		ReloadStrategy strategy = reloadProperties.getStrategy();
		if (strategy != null) {
			switch (strategy) {
			case RESTART_CONTEXT:
				restarter.orElseThrow(() -> new AssertionError("Restart endpoint is not enabled"));
				return new ConfigurationUpdateStrategy(() -> {
					wait(reloadProperties);
					restarter.get().restart();
				});
			case REFRESH:
				return new ConfigurationUpdateStrategy(refresher::refresh);
			}
			throw new IllegalStateException("Unsupported configuration update strategy: " + strategy);
		}
		throw new IllegalStateException("Configuration update strategy not set");
	}

	private ConfigurationUpdateStrategy(Runnable reloadProcedure) {
		Assert.notNull(reloadProcedure, "reloadProcedure cannot be null");
		this.reloadProcedure = reloadProcedure;
	}

	public void run() {
		this.reloadProcedure.run();
	}

	private static void wait(ReloadProperties properties) {
		long waitMillis = ThreadLocalRandom.current().nextLong(properties.getMaxWaitForRestart().toMillis());
		try {
			Thread.sleep(waitMillis);
		}
		catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}
}
