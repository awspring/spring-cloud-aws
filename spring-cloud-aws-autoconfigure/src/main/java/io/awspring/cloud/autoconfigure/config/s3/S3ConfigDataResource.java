/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.autoconfigure.config.s3;

import java.util.Objects;
import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.core.style.ToStringCreator;

/**
 * Config data resource for AWS S3 integration.
 *
 * @author Kunal Varpe
 * @since 3.3.0
 */
public class S3ConfigDataResource extends ConfigDataResource {

	private final String context;

	private final boolean enabled;

	private final boolean optional;

	private final S3PropertySources propertySources;

	public S3ConfigDataResource(String context, boolean optional, S3PropertySources propertySources) {
		this(context, optional, true, propertySources);
	}

	public S3ConfigDataResource(String context, boolean optional, boolean enabled, S3PropertySources propertySources) {
		this.context = context;
		this.optional = optional;
		this.propertySources = propertySources;
		this.enabled = enabled;
	}

	/**
	 * Returns context which is equal to S3 bucket and property file.
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

	public boolean isEnabled() {
		return enabled;
	}

	public S3PropertySources getPropertySources() {
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
		S3ConfigDataResource that = (S3ConfigDataResource) o;
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
