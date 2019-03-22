/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.context.properties;

import com.amazonaws.auth.profile.internal.AwsProfileNameLoader;

/**
 * Properties related to AWS credentials.
 *
 * @author Tom Gianos
 * @since 2.0.2
 * @see org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration
 */
public class AwsCredentialsProperties {

	/**
	 * The access key to be used with a static provider.
	 */
	private String accessKey;

	/**
	 * The secret key to be used with a static provider.
	 */
	private String secretKey;

	/**
	 * Configures an instance profile credentials provider with no further configuration.
	 */
	private boolean instanceProfile = true;

	/**
	 * Use the DefaultAWSCredentials Chain instead of configuring a custom credentials
	 * chain.
	 */
	private boolean useDefaultAwsCredentialsChain;

	/**
	 * The AWS profile name.
	 */
	private String profileName = AwsProfileNameLoader.DEFAULT_PROFILE_NAME;

	/**
	 * The AWS profile path.
	 */
	private String profilePath;

	public String getAccessKey() {
		return this.accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return this.secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public boolean isInstanceProfile() {
		return this.instanceProfile;
	}

	public void setInstanceProfile(boolean instanceProfile) {
		this.instanceProfile = instanceProfile;
	}

	public boolean isUseDefaultAwsCredentialsChain() {
		return this.useDefaultAwsCredentialsChain;
	}

	public void setUseDefaultAwsCredentialsChain(boolean useDefaultAwsCredentialsChain) {
		this.useDefaultAwsCredentialsChain = useDefaultAwsCredentialsChain;
	}

	public String getProfileName() {
		return this.profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getProfilePath() {
		return this.profilePath;
	}

	public void setProfilePath(String profilePath) {
		this.profilePath = profilePath;
	}

}
