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
package io.awspring.cloud.appconfig;

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
	private String context;

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

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String configurationProfileIdentifier;
		private String environmentIdentifier;
		private String applicationIdentifier;
		private String context;

		private Builder() {
		}

		public Builder configurationProfileIdentifier(String configurationProfileIdentifier) {
			this.configurationProfileIdentifier = configurationProfileIdentifier;
			return this;
		}

		public Builder environmentIdentifier(String environmentIdentifier) {
			this.environmentIdentifier = environmentIdentifier;
			return this;
		}

		public Builder applicationIdentifier(String applicationIdentifier) {
			this.applicationIdentifier = applicationIdentifier;
			return this;
		}

		public Builder context(String context) {
			this.context = context;
			return this;
		}

		public RequestContext build() {
			RequestContext requestContext = new RequestContext();
			requestContext.setConfigurationProfileIdentifier(configurationProfileIdentifier);
			requestContext.setEnvironmentIdentifier(environmentIdentifier);
			requestContext.setApplicationIdentifier(applicationIdentifier);
			requestContext.setContext(context);
			return requestContext;
		}
	}
}
