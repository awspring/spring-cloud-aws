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
package io.awspring.cloud.autoconfigure.core;

import org.springframework.lang.Nullable;
/**
 * Properties related to AWS Container Credentials for PodIdentity. The properties are not configured, it will default to the EKS values
 * from: <a href="https://docs.aws.amazon.com/eks/latest/userguide/pod-id-configure-pods.html">
 *
 * @author YoungJin Jung
 */
public class PodIdentityProperties {
	/**
	 * The full URI path to a localhost metadata service that will be used by credentials provider. By default, this will be
	 * read from {@link software.amazon.awssdk.core.SdkSystemSetting}.
	 */
	@Nullable
	private String containerCredentialsFullUri;
	/**
	 * Enables provider to asynchronously fetch credentials in the background. Defaults to synchronous blocking if not
	 * specified otherwise.
	 */
	private boolean asyncCredentialsUpdate = false;

	@Nullable
	public String getContainerCredentialsFullUri() {
		return containerCredentialsFullUri;
	}

	public void setContainerCredentialsFullUri(@Nullable String containerCredentialsFullUri) {
		this.containerCredentialsFullUri = containerCredentialsFullUri;
	}

	public boolean isAsyncCredentialsUpdate() {
		return asyncCredentialsUpdate;
	}

	public void setAsyncCredentialsUpdate(boolean asyncCredentialsUpdate) {
		this.asyncCredentialsUpdate = asyncCredentialsUpdate;
	}
}
