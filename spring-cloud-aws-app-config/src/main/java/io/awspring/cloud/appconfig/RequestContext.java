/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.Objects;

/**
 * Context used by {@link AppConfigPropertySource} to construct a call to AppConfig.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class RequestContext {
	/**
	 * ConfigurationProfileIdentifier or ConfigurationProfileName that is used to fetch SessionToken.
	 */
	private String configurationProfileIdentifier;
	/**
	 * EnvironmentIdentifier or EnvironmentName that is used to fetch SessionToken.
	 */
	private String environmentIdentifier;
	/**
	 * ApplicationIdentifier or ApplicationName that is used to fetch SessionToken.
	 */
	private String applicationIdentifier;
	/**
	 * Full context name which was used to extract values configurationProfileIdentifier, environmentIdentifier and
	 * applicationIdentifier.
	 */
	private String context;

	public RequestContext(String configurationProfileIdentifier, String environmentIdentifier,
			String applicationIdentifier, String context) {
		this.configurationProfileIdentifier = configurationProfileIdentifier;
		this.environmentIdentifier = environmentIdentifier;
		this.applicationIdentifier = applicationIdentifier;
		this.context = context;
	}

	public String getConfigurationProfileIdentifier() {
		return configurationProfileIdentifier;
	}

	public void setConfigurationProfileIdentifier(String configurationProfileIdentifier) {
		this.configurationProfileIdentifier = configurationProfileIdentifier;
	}

	public String getEnvironmentIdentifier() {
		return environmentIdentifier;
	}

	public void setEnvironmentIdentifier(String environmentIdentifier) {
		this.environmentIdentifier = environmentIdentifier;
	}

	public String getApplicationIdentifier() {
		return applicationIdentifier;
	}

	public void setApplicationIdentifier(String applicationIdentifier) {
		this.applicationIdentifier = applicationIdentifier;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null || getClass() != object.getClass())
			return false;
		RequestContext that = (RequestContext) object;
		return Objects.equals(configurationProfileIdentifier, that.configurationProfileIdentifier)
				&& Objects.equals(environmentIdentifier, that.environmentIdentifier)
				&& Objects.equals(applicationIdentifier, that.applicationIdentifier)
				&& Objects.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configurationProfileIdentifier, environmentIdentifier, applicationIdentifier, context);
	}
}
