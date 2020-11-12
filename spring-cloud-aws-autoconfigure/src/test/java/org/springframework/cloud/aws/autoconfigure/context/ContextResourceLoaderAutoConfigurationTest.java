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

package org.springframework.cloud.aws.autoconfigure.context;

import java.net.URI;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsS3ResourceLoaderProperties;
import org.springframework.cloud.aws.context.support.io.SimpleStorageProtocolResolverConfigurer;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ContextResourceLoaderAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ContextResourceLoaderAutoConfiguration.class));

	@Test
	void createResourceLoader_withCustomTaskExecutorSettings_executorConfigured() {
		// Arrange
		this.contextRunner.withPropertyValues("cloud.aws.loader.corePoolSize:10", "cloud.aws.loader.maxPoolSize:20",
				"cloud.aws.loader.queueCapacity:0").run(context -> {
					assertThat(context).hasSingleBean(AwsS3ResourceLoaderProperties.class);
					assertThat(context).hasSingleBean(AmazonS3Client.class);

					SimpleStorageProtocolResolverConfigurer simpleStorageProtocolResolverConfigurer = context
							.getBean(SimpleStorageProtocolResolverConfigurer.class);

					SimpleStorageProtocolResolver simpleStorageProtocolResolver = (SimpleStorageProtocolResolver) ReflectionTestUtils
							.getField(simpleStorageProtocolResolverConfigurer, "protocolResolver");

					ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ReflectionTestUtils
							.getField(simpleStorageProtocolResolver, "taskExecutor");

					assertThat(taskExecutor).isNotNull();

					assertThat(taskExecutor.getCorePoolSize()).isEqualTo(10);
					assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(20);
					assertThat(ReflectionTestUtils.getField(taskExecutor, "queueCapacity")).isEqualTo(0);
				});
	}

	@Test
	void createResourceLoader_withoutExecutorSettings_executorConfigured() {

		this.contextRunner.withPropertyValues().run(context -> {
			assertThat(context).hasSingleBean(AwsS3ResourceLoaderProperties.class);
			assertThat(context).hasSingleBean(AmazonS3Client.class);

			SimpleStorageProtocolResolverConfigurer simpleStorageProtocolResolverConfigurer = context
					.getBean(SimpleStorageProtocolResolverConfigurer.class);

			SimpleStorageProtocolResolver simpleStorageProtocolResolver = (SimpleStorageProtocolResolver) ReflectionTestUtils
					.getField(simpleStorageProtocolResolverConfigurer, "protocolResolver");

			SyncTaskExecutor taskExecutor = (SyncTaskExecutor) ReflectionTestUtils
					.getField(simpleStorageProtocolResolver, "taskExecutor");

			assertThat(taskExecutor).isNotNull();
		});

	}

	@Test
	void enableS3withCustomEndpoint() {
		this.contextRunner.withPropertyValues("cloud.aws.s3.endpoint:http://localhost:8090").run((context) -> {
			AmazonS3 client = context.getBean(AmazonS3.class);
			Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
			assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

			Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client, "isEndpointOverridden");
			assertThat(isEndpointOverridden).isTrue();
		});
	}

	@Test
	void enableS3withSpecificRegion() {
		this.contextRunner.withPropertyValues("cloud.aws.s3.region:us-east-1").run((context) -> {
			AmazonS3 client = context.getBean(AmazonS3.class);
			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.US_EAST_1.getName());
		});
	}

}
