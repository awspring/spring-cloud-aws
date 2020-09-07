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

package org.springframework.cloud.aws.appconfig;

import java.util.Objects;

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
 */
public class AwsAppConfigPropertySourceLocator implements PropertySourceLocator {

	private final AmazonAppConfig appConfigClient;

	private final AwsAppConfigProperties properties;

	private static final Log logger = LogFactory
			.getLog(AwsAppConfigPropertySourceLocator.class);

	public AwsAppConfigPropertySourceLocator(AmazonAppConfig appConfigClient,
			AwsAppConfigProperties properties) {
		this.appConfigClient = appConfigClient;
		this.properties = properties;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment)) {
			return null;
		}

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;

		String appName = this.properties.getConfigurationProfile();
		if (Objects.isNull(appName)) {
			appName = env.getProperty("spring.application.name");
		}

		CompositePropertySource composite = new CompositePropertySource("aws-app-config");

		try {
			composite.addPropertySource(create(appName));
		}
		catch (Exception ex) {
			if (properties.isFailFast()) {
				logger.error(
						"Fail fast is set and there was an error reading configuration from AWS AppConfig: {}",
						ex);
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			else {
				logger.warn("Unable to load AWS AppConfig from " + appName, ex);
			}
		}

		return composite;
	}

	private AwsAppConfigPropertySource create(String appName) {
		AwsAppConfigPropertySource propertySource = new AwsAppConfigPropertySource(
				appName, appConfigClient, properties);
		propertySource.init();

		return propertySource;
	}

}
