/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.kinesis;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.kinesis.config.KclMessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import io.awspring.cloud.kinesis.operations.KinesisTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KinesisAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, KinesisAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void isDisabledWhenPropertySetToFalse() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(KinesisTemplate.class));
	}

	@Test
	void createsClientsTemplateAndFactory() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(KinesisAsyncClient.class);
			assertThat(context).hasSingleBean(DynamoDbAsyncClient.class);
			assertThat(context).hasSingleBean(CloudWatchAsyncClient.class);
			assertThat(context).hasSingleBean(KinesisTemplate.class);
			assertThat(context).hasSingleBean(KclMessageListenerContainerFactory.class);
		});
	}

	@Test
	void bindsListenerProperties() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.kinesis.listener.max-records:250",
				"spring.cloud.aws.kinesis.listener.retrieval-mode:ENHANCED_FAN_OUT",
				"spring.cloud.aws.kinesis.listener.checkpoint-mode:MANUAL").run(context -> {
					KinesisProperties properties = context.getBean(KinesisProperties.class);
					assertThat(properties.getListener().getMaxRecords()).isEqualTo(250);
					assertThat(properties.getListener().getRetrievalMode()).isEqualTo(RetrievalMode.ENHANCED_FAN_OUT);
					assertThat(properties.getListener().getCheckpointMode()).isEqualTo(KclCheckpointMode.MANUAL);
				});
	}

}
