/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.testcontainers;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.core.region.StaticRegionProvider;
import io.floci.testcontainers.FlociContainer;
import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

/**
 * {@link AwsClientFactory} implementation that creates containers pointing to Floci instance running through
 * {@link FlociContainer}.
 *
 * @author Bastian Hellmann
 * @since 4.1.0
 */
public class FlociAwsClientFactory implements AwsClientFactory {
	private final AwsClientBuilderConfigurer configurer;

	public FlociAwsClientFactory(FlociContainer floci) {
		this.configurer = clientBuilderConfigurer(floci);
	}

	@Override
	public <CLIENT, BUILDER extends AwsClientBuilder<BUILDER, CLIENT>> CLIENT create(BUILDER builder) {
		return configurer.configure(builder).build();
	}

	private AwsClientBuilderConfigurer clientBuilderConfigurer(FlociContainer floci) {
		AwsProperties properties = new AwsProperties();
		properties.setEndpoint(URI.create(floci.getEndpoint()));

		StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider
				.create(AwsBasicCredentials.create(floci.getAccessKey(), floci.getSecretKey()));
		StaticRegionProvider regionProvider = new StaticRegionProvider(floci.getRegion());
		return new AwsClientBuilderConfigurer(credentialsProvider, regionProvider, properties);
	}
}
