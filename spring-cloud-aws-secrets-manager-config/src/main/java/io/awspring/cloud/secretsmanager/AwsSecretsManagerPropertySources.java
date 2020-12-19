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

package io.awspring.cloud.secretsmanager;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import org.apache.commons.logging.Log;

import org.springframework.util.StringUtils;

public class AwsSecretsManagerPropertySources {

	private final AwsSecretsManagerProperties properties;

	private final Log log;

	public AwsSecretsManagerPropertySources(AwsSecretsManagerProperties properties, Log log) {
		this.properties = properties;
		this.log = log;
	}

	public List<String> getAutomaticContexts(List<String> profiles) {
		List<String> contexts = new ArrayList<>();
		String prefix = this.properties.getPrefix();
		String defaultContext = getContext(prefix, this.properties.getDefaultContext());

		String appName = this.properties.getName();

		String appContext = prefix + "/" + appName;
		addProfiles(contexts, appContext, profiles);
		contexts.add(appContext);

		addProfiles(contexts, defaultContext, profiles);
		contexts.add(defaultContext);
		return contexts;
	}

	protected String getContext(String prefix, String context) {
		if (StringUtils.hasLength(prefix)) {
			return prefix + "/" + context;
		}
		return context;
	}

	private void addProfiles(List<String> contexts, String baseContext, List<String> profiles) {
		for (String profile : profiles) {
			contexts.add(baseContext + this.properties.getProfileSeparator() + profile);
		}
	}

	/**
	 * Creates property source for given context.
	 * @param context property source context equivalent to the secret name
	 * @param optional if creating context should fail with exception if secret cannot be
	 * loaded
	 * @param client Secret Manager client
	 * @return a property source or null if secret could not be loaded and optional is set
	 * to true
	 */
	public AwsSecretsManagerPropertySource createPropertySource(String context, boolean optional,
			AWSSecretsManager client) {
		log.info("Loading secrets from AWS Secret Manager secret with name: " + context + ", optional: " + optional);
		try {
			AwsSecretsManagerPropertySource propertySource = new AwsSecretsManagerPropertySource(context, client);
			propertySource.init();
			return propertySource;
			// TODO: howto call close when /refresh
		}
		catch (Exception e) {
			if (!optional) {
				throw new AwsSecretsManagerPropertySourceNotFoundException(e);
			}
			else {
				log.warn("Unable to load AWS secret from " + context + ". " + e.getMessage());
			}
		}
		return null;
	}

	static class AwsSecretsManagerPropertySourceNotFoundException extends RuntimeException {

		AwsSecretsManagerPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
