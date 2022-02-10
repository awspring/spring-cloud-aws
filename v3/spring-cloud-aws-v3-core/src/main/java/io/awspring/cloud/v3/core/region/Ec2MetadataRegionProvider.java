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

package io.awspring.cloud.v3.core.region;

import java.util.Objects;

import org.springframework.util.Assert;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.utils.StringUtils;

/**
 * {@link software.amazon.awssdk.regions.providers.AwsRegionProvider} implementation that dynamically
 * retrieves the region with the EC2 meta-data. This implementation allows application to
 * run against their region without any further configuration.
 *
 * @author Agim Emruli
 * @author Gleb Schukin
 */
public class Ec2MetadataRegionProvider implements AwsRegionProvider {

	@Override
	public Region getRegion() {
		Region currentRegion = getCurrentRegion();
		Assert.state(currentRegion != null,
				"There is no EC2 meta data available, because the application is not running "
						+ "in the EC2 environment. Region detection is only possible if the application is running on a EC2 instance");
		return currentRegion;
	}

	protected Region getCurrentRegion() {
		try {
			EC2MetadataUtils.InstanceInfo instanceInfo = EC2MetadataUtils.getInstanceInfo();
			return Objects.nonNull(instanceInfo) && StringUtils.isNotBlank(instanceInfo.getRegion())
				? Region.of(instanceInfo.getRegion()) : null;
		}
		catch (SdkClientException e) {
			return null;
		}

	}

}
