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

package io.awspring.cloud.appconfig;

import java.util.Arrays;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ReflectionUtils;

import static java.util.Objects.isNull;
import static org.springframework.util.Assert.hasText;

/**
 * @author jarpz
 */
public class AwsAppConfigPropertySourceLocator implements PropertySourceLocator {

	private final AmazonAppConfig appConfigClient;

	private final String clientId;

	private final String application;

	private final String configurationProfile;

	private final String environment;

	private final String configurationVersion;

	private final boolean failFast;

	private static final Log logger = LogFactory.getLog(AwsAppConfigPropertySourceLocator.class);

	public AwsAppConfigPropertySourceLocator(AmazonAppConfig appConfigClient, String clientId, String application,
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

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;

		String appName = configurationProfile;
		if (isNull(appName)) {
			appName = env.getProperty("spring.application.name");
		}
		String profile = this.environment;
		if (isNull(profile)) {
			profile = Arrays.stream(env.getActiveProfiles()).findFirst().orElse("default");
		}

		hasText(appName, "configurationProfile or spring.application.name should not be empty or null.");
		hasText(profile, "environment or profiles should not be empty or null ");

		CompositePropertySource composite = new CompositePropertySource("aws-app-config");

		try {
			composite.addPropertySource(create(appName, profile));
		}
		catch (Exception ex) {
			if (failFast) {
				logger.error("Fail fast is set and there was an error reading configuration from AWS AppConfig: {}",
						ex);
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			else {
				logger.warn("Unable to load AWS AppConfig from " + appName, ex);
			}
		}

		return composite;
	}

	private AwsAppConfigPropertySource create(String appName, String profile) {
		AwsAppConfigPropertySource propertySource = new AwsAppConfigPropertySource(appName, clientId, application,
				profile, configurationVersion, appConfigClient);
		propertySource.init();

		return propertySource;
	}

}
