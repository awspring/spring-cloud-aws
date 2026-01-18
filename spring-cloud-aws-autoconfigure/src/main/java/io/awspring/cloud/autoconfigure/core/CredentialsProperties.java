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
package io.awspring.cloud.autoconfigure.core;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties related to AWS credentials.
 *
 * @author Tom Gianos
 * @since 2.0.2
 */
@ConfigurationProperties(prefix = CredentialsProperties.PREFIX)
public class CredentialsProperties {

	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.credentials";

	/**
	 * The access key to be used with a static provider.
	 */
	@Nullable
	private String accessKey;

	/**
	 * The secret key to be used with a static provider.
	 */
	@Nullable
	private String secretKey;

	/**
	 * Configures an instance profile credentials provider with no further configuration.
	 */
	private boolean instanceProfile = false;

	/**
	 * The AWS profile.
	 */
	@NestedConfigurationProperty
	@Nullable
	private Profile profile;

	/**
	 * Properties used to configure STS
	 * {@link software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider}.
	 */
	@NestedConfigurationProperty
	@Nullable
	private StsProperties sts;

	@Nullable
	public String getAccessKey() {
		return this.accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	@Nullable
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

	@Nullable
	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	@Nullable
	public StsProperties getSts() {
		return sts;
	}

	public void setSts(@Nullable StsProperties sts) {
		this.sts = sts;
	}
}
