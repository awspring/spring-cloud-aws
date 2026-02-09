package io.awspring.cloud.appconfig;

import java.util.Objects;

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

	public RequestContext(String configurationProfileIdentifier, String environmentIdentifier, String applicationIdentifier, String context) {
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
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		RequestContext that = (RequestContext) object;
		return Objects.equals(configurationProfileIdentifier, that.configurationProfileIdentifier) && Objects.equals(environmentIdentifier, that.environmentIdentifier) && Objects.equals(applicationIdentifier, that.applicationIdentifier) && Objects.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configurationProfileIdentifier, environmentIdentifier, applicationIdentifier, context);
	}
}
