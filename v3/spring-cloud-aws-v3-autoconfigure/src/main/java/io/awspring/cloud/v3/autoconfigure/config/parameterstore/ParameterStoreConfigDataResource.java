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

package io.awspring.cloud.v3.autoconfigure.config.parameterstore;

import java.util.Objects;

import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

/**
 * Config data resource for AWS System Manager Management integration.
 *
 * @author Eddú Meléndez
 * @since 2.3.0
 */
public class ParameterStoreConfigDataResource extends ConfigDataResource {

	private final String context;

	private final boolean optional;

	private final ParameterStorePropertySources propertySources;

	public ParameterStoreConfigDataResource(String context, boolean optional,
			ParameterStorePropertySources propertySources) {
		this.context = context;
		this.optional = optional;
		this.propertySources = propertySources;
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

	public ParameterStorePropertySources getPropertySources() {
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
		ParameterStoreConfigDataResource that = (ParameterStoreConfigDataResource) o;
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
