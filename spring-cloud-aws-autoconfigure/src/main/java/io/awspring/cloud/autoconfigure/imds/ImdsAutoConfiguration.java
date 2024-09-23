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
package io.awspring.cloud.autoconfigure.imds;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.imds.Ec2MetadataClient;

/**
 * Configuration for managing Instance Meta Data Service metadata.
 *
 * @author Ken Krueger
 * @since 3.1.0
 */

@AutoConfiguration
@ConditionalOnClass(Ec2MetadataClient.class)
@ConditionalOnProperty(name = "spring.cloud.aws.imds.enabled", havingValue = "true", matchIfMissing = true)
public class ImdsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ImdsPropertySource imdsPropertySource(ConfigurableEnvironment env, ImdsUtils imdsUtils) {
		ImdsPropertySource propertySource = new ImdsPropertySource("Ec2InstanceMetadata", imdsUtils);
		propertySource.init();
		env.getPropertySources().addFirst(propertySource);
		return propertySource;
	}

	@Bean
	@ConditionalOnMissingBean
	public ImdsUtils imdsUtils(Ec2MetadataClient ec2MetadataClient) {
		return new ImdsUtils(ec2MetadataClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public Ec2MetadataClient ec2MetadataClient() {
		return Ec2MetadataClient.create();
	}

}
