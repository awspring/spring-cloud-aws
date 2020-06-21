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

package org.springframework.cloud.aws.paramstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ReflectionUtils;

/**
 * Builds a {@link CompositePropertySource} with various
 * {@link AwsParamStorePropertySource} instances based on active profiles, application
 * name and default context permutations. Mostly copied from Spring Cloud Consul's config
 * support, but without the option to have full config files in a param value: with the
 * AWS Parameter Store that wouldn't make sense, given the maximum size limit of 4096
 * characters for a parameter value.
 *
 * @author Joris Kuipers
 * @author Matej Nedic
 * @since 2.0.0
 */
public class AwsParamStorePropertySourceLocator implements PropertySourceLocator {

	private AWSSimpleSystemsManagement ssmClient;

	private AwsParamStoreProperties properties;

	private final Set<String> contexts = new LinkedHashSet<>();

	private Log logger = LogFactory.getLog(getClass());

	public AwsParamStorePropertySourceLocator(AWSSimpleSystemsManagement ssmClient,
			AwsParamStoreProperties properties) {
		this.ssmClient = ssmClient;
		this.properties = properties;
	}

	public List<String> getContexts() {
		return new ArrayList<>(contexts);
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment)) {
			return null;
		}

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;

		String appName = properties.getName();

		if (appName == null) {
			appName = env.getProperty("spring.application.name");
		}

		List<String> profiles = Arrays.asList(env.getActiveProfiles());

		String prefix = this.properties.getPrefix();

		String appContext = prefix + "/" + appName;
		addProfiles(this.contexts, appContext, profiles);
		this.contexts.add(appContext + "/");

		String defaultContext = prefix + "/" + this.properties.getDefaultContext();
		addProfiles(this.contexts, defaultContext, profiles);
		this.contexts.add(defaultContext + "/");

		CompositePropertySource composite = new CompositePropertySource(
				"aws-param-store");

		for (String propertySourceContext : this.contexts) {
			try {
				composite.addPropertySource(create(propertySourceContext));
			}
			catch (Exception e) {
				if (this.properties.isFailFast()) {
					logger.error(
							"Fail fast is set and there was an error reading configuration from AWS Parameter Store:\n"
									+ e.getMessage());
					ReflectionUtils.rethrowRuntimeException(e);
				}
				else {
					logger.warn("Unable to load AWS config from " + propertySourceContext,
							e);
				}
			}
		}

		return composite;
	}

	private AwsParamStorePropertySource create(String context) {
		AwsParamStorePropertySource propertySource = new AwsParamStorePropertySource(
				context, this.ssmClient);
		propertySource.init();
		return propertySource;
	}

	private void addProfiles(Set<String> contexts, String baseContext,
			List<String> profiles) {
		for (String profile : profiles) {
			contexts.add(
					baseContext + this.properties.getProfileSeparator() + profile + "/");
		}
	}

}
