/*
 * Copyright 2013-2025 the original author or authors.
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

import static io.awspring.cloud.autoconfigure.kinesis.KinesisProperties.PREFIX;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Properties related to KinesisClient
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class KinesisProperties extends AwsClientProperties {
	/**
	 * The prefix used for AWS Kinesis configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.kinesis";

	@Nullable
	private KinesisProducerProperties producer = new KinesisProducerProperties();

	public KinesisProducerProperties getProducer() {
		return producer;
	}

	public void setProducer(KinesisProducerProperties producer) {
		this.producer = producer;
	}
}
