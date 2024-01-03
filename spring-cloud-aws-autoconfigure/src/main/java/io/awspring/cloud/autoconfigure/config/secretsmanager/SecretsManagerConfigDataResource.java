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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

import java.util.Objects;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

/**
 * Config data resource for AWS Secret Manager integration.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @since 2.3.0
 */
public class SecretsManagerConfigDataResource extends ConfigDataResource {

	private final String context;

	private final boolean optional;

	/**
	 * If resource should be resolved. This flag has the same value as {@link SecretsManagerProperties#isEnabled()}.
	 */
	private final boolean enabled;

	private final SecretsManagerPropertySources propertySources;

	public SecretsManagerConfigDataResource(String context, boolean optional, boolean enabled,
			SecretsManagerPropertySources propertySources) {
		this.context = context;
		this.optional = optional;
		this.enabled = enabled;
		this.propertySources = propertySources;
	}

	public SecretsManagerConfigDataResource(String context, boolean optional,
			SecretsManagerPropertySources propertySources) {
		this(context, optional, true, propertySources);
	}

	/**
	 * Returns context which is equal to Secret Manager secret name.
	 * @return the context
	 */
	public String getContext() {
		return this.context;
	}

	/**
	 * If application startup should fail when secret cannot be loaded or does not exist.
	 * @return is optional
	 */
	public boolean isOptional() {
		return this.optional;
	}

	boolean isEnabled() {
		return enabled;
	}

	public SecretsManagerPropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SecretsManagerConfigDataResource that = (SecretsManagerConfigDataResource) o;
		return optional == that.optional && enabled == that.enabled && Objects.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, optional, enabled);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("context", context).append("optional", optional)
				.append("skipped", enabled).toString();

	}

}
