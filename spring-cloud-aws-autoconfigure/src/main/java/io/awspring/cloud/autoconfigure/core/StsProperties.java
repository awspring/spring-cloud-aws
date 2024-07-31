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
 * Properties related to AWS Sts Credentials. It the properties are not configured, it will default to the EKS values
 * from: <a href="https://docs.aws.amazon.com/eks/latest/userguide/pod-configuration.html">
 *
 * @author Eduan Bekker
 * @since 3.0.0
 */
public class StsProperties {

	/**
	 * ARN of IAM role associated with STS. If not provided this will be read from
	 * {@link software.amazon.awssdk.core.SdkSystemSetting}.
	 */
	@Nullable
	private String roleArn;

	/**
	 * Absolute path to the web identity token file that will be used by credentials provider. By default this will be
	 * read from {@link software.amazon.awssdk.core.SdkSystemSetting}.
	 */
	@Nullable
	private String webIdentityTokenFile;

	/**
	 * Enables provider to asynchronously fetch credentials in the background. Defaults to synchronous blocking if not
	 * specified otherwise.
	 */
	private boolean asyncCredentialsUpdate = false;

	/**
	 * Role session name that will be used by credentials provider. By default this is read from
	 * {@link software.amazon.awssdk.core.SdkSystemSetting}.
	 */
	@Nullable
	private String roleSessionName;

	public boolean isAsyncCredentialsUpdate() {
		return asyncCredentialsUpdate;
	}

	@Nullable
	public String getRoleSessionName() {
		return roleSessionName;
	}

	@Nullable
	public String getRoleArn() {
		return roleArn;
	}

	@Nullable
	public String getWebIdentityTokenFile() {
		return webIdentityTokenFile;
	}

	public void setRoleArn(@Nullable String roleArn) {
		this.roleArn = roleArn;
	}

	public void setWebIdentityTokenFile(@Nullable String webIdentityTokenFile) {
		this.webIdentityTokenFile = webIdentityTokenFile;
	}

	public void setAsyncCredentialsUpdate(boolean asyncCredentialsUpdate) {
		this.asyncCredentialsUpdate = asyncCredentialsUpdate;
	}

	public void setRoleSessionName(@Nullable String roleSessionName) {
		this.roleSessionName = roleSessionName;
	}
}
