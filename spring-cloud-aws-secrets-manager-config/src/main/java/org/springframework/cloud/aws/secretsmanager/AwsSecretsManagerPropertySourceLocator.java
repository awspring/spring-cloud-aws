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

package org.springframework.cloud.aws.secretsmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Builds a {@link CompositePropertySource} with various
 * {@link AwsSecretsManagerPropertySource} instances based on active profiles, application
 * name and default context permutations. Mostly copied from Spring Cloud Consul's config
 * support.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.0.0
 */
public class AwsSecretsManagerPropertySourceLocator implements PropertySourceLocator {

	private final String propertySourceName;

	private final AWSSecretsManager smClient;

	private final AwsSecretsManagerProperties properties;

	private final Set<String> contexts = new LinkedHashSet<>();

	private Log logger = LogFactory.getLog(getClass());

	public AwsSecretsManagerPropertySourceLocator(String propertySourceName, AWSSecretsManager smClient,
			AwsSecretsManagerProperties properties) {
		this.propertySourceName = propertySourceName;
		this.smClient = smClient;
		this.properties = properties;
	}

	public AwsSecretsManagerPropertySourceLocator(AWSSecretsManager smClient, AwsSecretsManagerProperties properties) {
		this("aws-secrets-manager", smClient, properties);
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

		AwsSecretsManagerPropertySources sources = new AwsSecretsManagerPropertySources(properties, logger);

		List<String> profiles = Arrays.asList(env.getActiveProfiles());
		this.contexts.addAll(sources.getAutomaticContexts(profiles));

		CompositePropertySource composite = new CompositePropertySource(this.propertySourceName);

		for (String propertySourceContext : this.contexts) {
			PropertySource<AWSSecretsManager> propertySource = sources.createPropertySource(propertySourceContext, true,
					this.smClient);
			composite.addPropertySource(propertySource);
		}

		return composite;
	}

}
