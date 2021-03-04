/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cloudmap;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.cloudmap.CloudMapProperties;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapPropertySourceLocator;
import org.springframework.cloud.aws.cloudmap.CloudMapDiscoverService;
import org.springframework.cloud.aws.cloudmap.CloudMapRegistryService;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsCloudMapBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AwsCloudMapBootstrapConfiguration.class));

	@Test
	void testWithStaticRegion() {
		this.contextRunner

				.withPropertyValues("aws.cloudmap.enabled:true",
						"aws.cloudmap.discovery.discoveryList[0].serviceNameSpace:namespace",
						"aws.cloudmap.discovery.discoveryList[0].service:service")
				.run(context -> {
					assertThat(context).hasSingleBean(AwsCloudMapPropertySourceLocator.class);
					assertThat(context).hasSingleBean(AWSServiceDiscovery.class);
					assertThat(context).hasSingleBean(CloudMapDiscoverService.class);
					assertThat(context).hasSingleBean(CloudMapRegistryService.class);

					CloudMapProperties properties = context.getBean(CloudMapProperties.class);

					assertThat(properties.getDiscovery().getDiscoveryList().get(0).getService()).isEqualTo("service");
				});
	}

}
