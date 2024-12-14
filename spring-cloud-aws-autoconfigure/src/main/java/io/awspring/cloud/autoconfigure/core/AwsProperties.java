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

import static io.awspring.cloud.autoconfigure.core.AwsProperties.CONFIG_PREFIX;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;

/**
 * Configuration properties for AWS environment.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
@ConfigurationProperties(CONFIG_PREFIX)
public class AwsProperties {
	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws";

	/**
	 * Overrides the default endpoint for all auto-configured AWS clients.
	 */
	@Nullable
	private URI endpoint;

	/**
	 * Sets the {@link DefaultsMode} that will be used to determine how certain default configuration options are
	 * resolved in the SDK.
	 *
	 * <a href=
	 * "https://aws.amazon.com/blogs/developer/introducing-smart-configuration-defaults-in-the-aws-sdk-for-java-v2/">Introducing
	 * Smart Configuration Defaults in the AWS SDK for Java v2</a>
	 */
	@Nullable
	private DefaultsMode defaultsMode;

	/**
	 * Configure whether the SDK should use the AWS dualstack endpoint. Note that not each AWS service supports
	 * dual-stack. For complete list check
	 * <a href="https://docs.aws.amazon.com/vpc/latest/userguide/aws-ipv6-support.html">AWS services that support
	 * IPv6</a> If you intend to use dual stack only on specific service, consider configuring dual stack through
	 * `spring.cloud.aws.<service-name>.dualstack-enabled` property.
	 */
	@Nullable
	private Boolean dualstackEnabled;

	/**
	 * Configure whether the SDK should use the AWS fips endpoints.
	 */
	@Nullable
	private Boolean fipsEnabled;

	@Nullable
	public URI getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable URI endpoint) {
		this.endpoint = endpoint;
	}

	@Nullable
	public DefaultsMode getDefaultsMode() {
		return defaultsMode;
	}

	public void setDefaultsMode(@Nullable DefaultsMode defaultsMode) {
		this.defaultsMode = defaultsMode;
	}

	@Nullable
	public Boolean getDualstackEnabled() {
		return dualstackEnabled;
	}

	public void setDualstackEnabled(@Nullable Boolean dualstackEnabled) {
		this.dualstackEnabled = dualstackEnabled;
	}

	@Nullable
	public Boolean getFipsEnabled() {
		return fipsEnabled;
	}

	public void setFipsEnabled(@Nullable Boolean fipsEnabled) {
		this.fipsEnabled = fipsEnabled;
	}
}
