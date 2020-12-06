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

package io.awspring.cloud.appconfig;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ReflectionUtils;

/**
 * @author jarpz
 * @author Eddú Meléndez
 */
public class AppConfigPropertySourceLocator implements PropertySourceLocator {

	private final AmazonAppConfig appConfigClient;

	private final String clientId;

	private final String application;

	private final String configurationProfile;

	private final String environment;

	private final String configurationVersion;

	private final boolean failFast;

	private static final Log logger = LogFactory.getLog(AppConfigPropertySourceLocator.class);

	public AppConfigPropertySourceLocator(AmazonAppConfig appConfigClient, String clientId, String application,
			String configurationProfile, String environment, String configurationVersion, boolean failFast) {
		this.appConfigClient = appConfigClient;
		this.clientId = clientId;
		this.application = application;
		this.configurationProfile = configurationProfile;
		this.environment = environment;
		this.configurationVersion = configurationVersion;
		this.failFast = failFast;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment)) {
			return null;
		}

		CompositePropertySource composite = new CompositePropertySource("aws-app-config");

		try {
			composite.addPropertySource(create(this.configurationProfile));
		}
		catch (Exception ex) {
			if (this.failFast) {
				logger.error("Fail fast is set and there was an error reading configuration from AWS AppConfig: {}",
						ex);
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			else {
				logger.warn("Unable to load AWS AppConfig from " + this.configurationProfile, ex);
			}
		}

		return composite;
	}

	private AppConfigPropertySource create(String configurationProfile) {
		AppConfigPropertySource propertySource = new AppConfigPropertySource(configurationProfile, this.clientId,
				this.application, this.environment, this.configurationVersion, this.appConfigClient);
		propertySource.init();

		return propertySource;
	}

}
