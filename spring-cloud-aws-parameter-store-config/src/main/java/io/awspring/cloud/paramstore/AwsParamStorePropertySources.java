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

package io.awspring.cloud.paramstore;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.apache.commons.logging.Log;

/**
 * Is responsible for creating {@link AwsParamStorePropertySource} and determining
 * automatic contexts.
 *
 * @author Eddú Meléndez
 * @author Manuel Wessner
 * @since 2.3
 */
public class AwsParamStorePropertySources {

	private final AwsParamStoreProperties properties;

	private final Log log;

	public AwsParamStorePropertySources(AwsParamStoreProperties properties, Log log) {
		this.properties = properties;
		this.log = log;
	}

	/**
	 * Returns a list of contexts applicable to profiles in <strong>ascending priority
	 * order</strong>.
	 *
	 * For example: when profile `dev1` is active and application name is set to `MyApp`,
	 * it returns a list containing:
	 *
	 * [0] /config/application/ [1] /config/application_dev1/ [2] /config/MyApp/ [3]
	 * /config/MyApp_dev1/
	 * @param profiles - active profiles
	 * @return list of contexts in <strong>ascending priority order</strong>
	 */
	public List<String> getAutomaticContexts(List<String> profiles) {
		List<String> contexts = new ArrayList<>();
		String prefix = this.properties.getPrefix();
		String defaultContext = getContext(prefix, this.properties.getDefaultContext());

		contexts.add(defaultContext + "/");
		addProfiles(contexts, defaultContext, profiles);

		String appName = this.properties.getName();
		String appContext = prefix + "/" + appName;
		contexts.add(appContext + "/");
		addProfiles(contexts, appContext, profiles);

		return contexts;
	}

	private String getContext(String prefix, String context) {
		if (prefix != null) {
			return prefix + "/" + context;
		}
		return context;
	}

	private void addProfiles(List<String> contexts, String baseContext, List<String> profiles) {
		for (String profile : profiles) {
			contexts.add(baseContext + this.properties.getProfileSeparator() + profile + "/");
		}
	}

	/**
	 * Creates property source for given context.
	 * @param context property source context equivalent to the parameter name
	 * @param optional if creating context should fail with exception if parameter cannot
	 * be loaded
	 * @param client System Manager Management client
	 * @return a property source or null if parameter could not be loaded and optional is
	 * set to true
	 */
	public AwsParamStorePropertySource createPropertySource(String context, boolean optional,
			AWSSimpleSystemsManagement client) {
		log.info("Loading property from AWS Parameter Store with name: " + context + ", optional: " + optional);
		try {
			AwsParamStorePropertySource propertySource = new AwsParamStorePropertySource(context, client);
			propertySource.init();
			return propertySource;
			// TODO: howto call close when /refresh
		}
		catch (Exception e) {
			if (!optional) {
				throw new AwsParameterPropertySourceNotFoundException(e);
			}
			else {
				log.warn("Unable to load AWS parameter from " + context + ". " + e.getMessage());
			}
		}
		return null;
	}

	static class AwsParameterPropertySourceNotFoundException extends RuntimeException {

		AwsParameterPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
