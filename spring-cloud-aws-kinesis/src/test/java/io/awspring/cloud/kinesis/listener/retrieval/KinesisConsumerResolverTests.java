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
package io.awspring.cloud.kinesis.listener.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KinesisConsumerResolverTests {

	private static final String CONSUMER_ARN = "arn:aws:kinesis:eu-west-1:123456789012:stream/orders/consumer/orders-efo:1234567890";

	@Test
	@DisplayName("a value starting with arn: is resolved as an ARN")
	void resolvesArn() {
		assertThat(KinesisConsumerResolver.resolveConsumerArn(CONSUMER_ARN)).isEqualTo(CONSUMER_ARN);
		assertThat(KinesisConsumerResolver.resolveConsumerName(CONSUMER_ARN)).isNull();
		assertThat(KinesisConsumerResolver.isArn(CONSUMER_ARN)).isTrue();
	}

	@Test
	@DisplayName("ARN detection is case insensitive")
	void resolvesUpperCaseArn() {
		String arn = "ARN:aws:kinesis:eu-west-1:123456789012:stream/orders";

		assertThat(KinesisConsumerResolver.resolveConsumerArn(arn)).isEqualTo(arn);
		assertThat(KinesisConsumerResolver.resolveConsumerName(arn)).isNull();
	}

	@Test
	@DisplayName("any other value is resolved as a consumer name")
	void resolvesName() {
		assertThat(KinesisConsumerResolver.resolveConsumerName("orders-consumer")).isEqualTo("orders-consumer");
		assertThat(KinesisConsumerResolver.resolveConsumerArn("orders-consumer")).isNull();
		assertThat(KinesisConsumerResolver.isArn("orders-consumer")).isFalse();
	}

	@Test
	@DisplayName("an empty value is rejected")
	void rejectsEmptyValue() {
		assertThatThrownBy(() -> KinesisConsumerResolver.resolveConsumerName(" "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("consumer name or ARN must not be empty");
	}

}
