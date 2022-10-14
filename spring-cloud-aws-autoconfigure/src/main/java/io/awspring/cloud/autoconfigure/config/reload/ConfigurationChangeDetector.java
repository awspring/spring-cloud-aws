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

import io.awspring.cloud.autoconfigure.config.secretsmanager.ReloadableProperties;
import java.util.Objects;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.log.LogAccessor;

public abstract class ConfigurationChangeDetector {

	private static final LogAccessor LOG = new LogAccessor(LogFactory.getLog(ConfigurationChangeDetector.class));

	protected ReloadableProperties properties;

	protected ConfigurationUpdateStrategy strategy;

	protected ConfigurableEnvironment environment;

	public ConfigurationChangeDetector(ReloadableProperties properties, ConfigurationUpdateStrategy strategy,
			ConfigurableEnvironment environment) {
		this.properties = Objects.requireNonNull(properties);
		this.strategy = Objects.requireNonNull(strategy);
		this.environment = environment;
	}

	public void reloadProperties() {
		LOG.info(() -> "Reloading using strategy: " + this.strategy.getName());
		strategy.getReloadProcedure().run();
	}

}
