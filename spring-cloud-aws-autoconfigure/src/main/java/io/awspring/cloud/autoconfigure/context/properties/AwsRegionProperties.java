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

package io.awspring.cloud.autoconfigure.context.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Properties related to AWS region configuration.
 *
 * @author Tom Gianos
 * @author Maciej Walkowiak
 * @since 2.0.2
 * @see io.awspring.cloud.autoconfigure.context.ContextRegionProviderAutoConfiguration
 */
@ConfigurationProperties(prefix = AwsRegionProperties.PREFIX)
public class AwsRegionProperties {

	/**
	 * The prefix used for AWS region related properties.
	 */
	public static final String PREFIX = "cloud.aws.region";

	/**
	 * Configures a static region for the application. Possible regions are (currently)
	 * us-east-1, us-west-1, us-west-2, eu-west-1, eu-central-1, ap-southeast-1,
	 * ap-southeast-1, ap-northeast-1, sa-east-1, cn-north-1 and any custom region
	 * configured with own region meta data.
	 */
	private String staticRegion;

	public String getStatic() {
		return this.staticRegion;
	}

	public boolean isStatic() {
		return StringUtils.hasText(this.staticRegion);
	}

	public void setStatic(String staticRegion) {
		// Feels like validation should be done to make sure this is a valid AWS region
		// value. However current
		// configuration in ContextRegionProviderAutoConfiguration doesn't seem to check
		// property is valid before
		// creating a bean definition. Leaving for now.
		// - tgianos 11/26/2018
		this.staticRegion = staticRegion;
	}

}
