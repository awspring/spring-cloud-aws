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
package io.awspring.cloud.autoconfigure;

import java.net.URI;
import org.springframework.lang.Nullable;

/**
 * Base properties for AWS Service client.
 *
 * @author Maciej Walkowiak
 */
public abstract class AwsClientProperties {

	/**
	 * Overrides the default endpoint.
	 */
	@Nullable
	private URI endpoint;

	/**
	 * Overrides the default region.
	 */
	@Nullable
	private String region;

	@Nullable
	public URI getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(URI endpoint) {
		this.endpoint = endpoint;
	}

	@Nullable
	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
}
