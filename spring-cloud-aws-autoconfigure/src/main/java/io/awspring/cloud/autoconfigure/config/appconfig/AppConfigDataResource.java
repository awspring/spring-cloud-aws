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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.RequestContext;
import java.util.Objects;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

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

	public AppConfigDataResource(RequestContext context, boolean optional, boolean enabled,
			AppConfigPropertySources propertySources) {
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
