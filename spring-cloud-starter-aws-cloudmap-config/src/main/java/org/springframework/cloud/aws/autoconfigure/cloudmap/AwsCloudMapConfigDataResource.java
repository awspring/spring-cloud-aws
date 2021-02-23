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

package org.springframework.cloud.aws.autoconfigure.cloudmap;

import java.util.Objects;

import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapPropertySources;
import org.springframework.core.style.ToStringCreator;

public class AwsCloudMapConfigDataResource extends ConfigDataResource {

	private final boolean optional;

	private final AwsCloudMapPropertySources propertySources;

	public AwsCloudMapConfigDataResource(boolean optional, AwsCloudMapPropertySources propertySources) {
		this.optional = optional;
		this.propertySources = propertySources;
	}

	public boolean isOptional() {
		return this.optional;
	}

	public AwsCloudMapPropertySources getPropertySources() {
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
		AwsCloudMapConfigDataResource that = (AwsCloudMapConfigDataResource) o;
		return this.optional == that.optional;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.optional);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("optional", optional).toString();
	}

}
