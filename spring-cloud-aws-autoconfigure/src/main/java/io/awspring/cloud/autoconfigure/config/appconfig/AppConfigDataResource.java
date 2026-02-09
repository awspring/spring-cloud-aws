package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.RequestContext;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

import java.util.Objects;

/**
 * Config data resource for AWS App Config integration.
 * @author Matej Nedic
 * @since 4.1.0
 */
public class AppConfigDataResource extends ConfigDataResource {

	private final RequestContext context;

	private final boolean enabled;

	private final boolean optional;

	private final AppConfigPropertySources propertySources;

	public AppConfigDataResource(RequestContext context, boolean optional, AppConfigPropertySources propertySources) {
		this(context, optional, true, propertySources);
	}

	public AppConfigDataResource(RequestContext context, boolean optional, boolean enabled, AppConfigPropertySources propertySources) {
		this.context = context;
		this.optional = optional;
		this.propertySources = propertySources;
		this.enabled = enabled;
	}

	/**
	 * Returns context which is equal to S3 bucket and property file.
	 *
	 * @return the context
	 */
	public RequestContext getContext() {
		return this.context;
	}

	/**
	 * If application startup should fail when secret cannot be loaded or does not exist.
	 *
	 * @return is optional
	 */
	public boolean isOptional() {
		return this.optional;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public AppConfigPropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AppConfigDataResource that = (AppConfigDataResource) o;
		return this.optional == that.optional && this.context.equals(that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.optional, this.context);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("context", context).append("optional", optional).toString();

	}
}
