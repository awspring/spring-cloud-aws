/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3.properties;

public class S3PluginProperties {

	/**
	 * If set to false if Access Grants does not find/return permissions, S3Client won't try to determine if policies
	 * grant access If set to true fallback policies S3/IAM will be evaluated.
	 */
	private boolean enableFallback;

	public boolean getEnableFallback() {
		return enableFallback;
	}

	public void setEnableFallback(boolean enableFallback) {
		this.enableFallback = enableFallback;
	}
}
