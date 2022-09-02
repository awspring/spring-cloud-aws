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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Properties related to AWS region configuration.
 *
 * @author Tom Gianos
 * @author Maciej Walkowiak
 * @since 2.0.2
 */
@ConfigurationProperties(prefix = RegionProperties.PREFIX)
public class RegionProperties {

	/**
	 * The prefix used for AWS region related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.region";

	/**
	 * Configures a static region for the application. Possible regions are (currently) us-east-1, us-west-1, us-west-2,
	 * eu-west-1, eu-central-1, ap-southeast-1, ap-southeast-1, ap-northeast-1, sa-east-1, cn-north-1 and any custom
	 * region configured with own region meta data.
	 */
	@Nullable
	private String staticRegion;

	/**
	 * Configures an instance profile region provider with no further configuration.
	 */
	private boolean instanceProfile = false;

	/**
	 * The AWS profile.
	 */
	@Nullable
	private Profile profile;

	@Nullable
	public Profile getProfile() {
		return this.profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	@Nullable
	public String getStatic() {
		return this.staticRegion;
	}

	public boolean isStatic() {
		return StringUtils.hasText(this.staticRegion);
	}

	public void setStatic(String staticRegion) {
		this.staticRegion = staticRegion;
	}

	public boolean isInstanceProfile() {
		return this.instanceProfile;
	}

	public void setInstanceProfile(boolean instanceProfile) {
		this.instanceProfile = instanceProfile;
	}

}
