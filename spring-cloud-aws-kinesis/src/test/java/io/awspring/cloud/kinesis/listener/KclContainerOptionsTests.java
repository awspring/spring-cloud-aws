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
package io.awspring.cloud.kinesis.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.SmartLifecycle;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
class KclContainerOptionsTests {

	@Test
	@DisplayName("builder applies sensible defaults")
	void appliesDefaults() {
		KclContainerOptions options = KclContainerOptions.builder().build();

		assertThat(options.getMaxRecords()).isEqualTo(10000);
		assertThat(options.getIdleTimeBetweenReadsInMillis()).isEqualTo(1000L);
		assertThat(options.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.TRIM_HORIZON);
		assertThat(options.getGracefulShutdownTimeout()).isEqualTo(Duration.ofSeconds(20));
		assertThat(options.getWorkerIdentifier()).isNotBlank();
	}

	@Test
	@DisplayName("builder overrides all values")
	void overridesValues() {
		KclContainerOptions options = KclContainerOptions.builder().maxRecords(500).idleTimeBetweenReadsInMillis(2000L)
				.initialPositionInStream(InitialPositionInStream.LATEST).workerIdentifier("worker-1")
				.gracefulShutdownTimeout(Duration.ofSeconds(5)).build();

		assertThat(options.getMaxRecords()).isEqualTo(500);
		assertThat(options.getIdleTimeBetweenReadsInMillis()).isEqualTo(2000L);
		assertThat(options.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.LATEST);
		assertThat(options.getWorkerIdentifier()).isEqualTo("worker-1");
		assertThat(options.getGracefulShutdownTimeout()).isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	@DisplayName("AT_TIMESTAMP without a timestamp fails fast at build time")
	void atTimestampWithoutTimestampFailsAtBuild() {
		assertThatThrownBy(() -> KclContainerOptions.builder()
				.initialPositionInStream(InitialPositionInStream.AT_TIMESTAMP).build())
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("initialPositionTimestamp");
	}

	@Test
	@DisplayName("AT_TIMESTAMP with a timestamp builds")
	void atTimestampWithTimestampBuilds() {
		Instant timestamp = Instant.ofEpochSecond(1_000);
		KclContainerOptions options = KclContainerOptions.builder()
				.initialPositionInStream(InitialPositionInStream.AT_TIMESTAMP).initialPositionTimestamp(timestamp)
				.build();

		assertThat(options.getInitialPositionInStream()).isEqualTo(InitialPositionInStream.AT_TIMESTAMP);
		assertThat(options.getInitialPositionTimestamp()).isEqualTo(timestamp);
	}

	@Test
	@DisplayName("lifecycle defaults are autoStartup=true and phase=DEFAULT_PHASE")
	void lifecycleDefaults() {
		KclContainerOptions options = KclContainerOptions.builder().build();

		assertThat(options.isAutoStartup()).isTrue();
		assertThat(options.getPhase()).isEqualTo(SmartLifecycle.DEFAULT_PHASE);
	}

	@Test
	@DisplayName("lifecycle, metrics namespace and fan-out consumer options are configurable")
	void configuresNewOptions() {
		KclContainerOptions options = KclContainerOptions.builder().autoStartup(false).phase(42)
				.metricsNamespace("my-namespace").consumerArn("arn:aws:kinesis:consumer").consumerName("my-consumer")
				.build();

		assertThat(options.isAutoStartup()).isFalse();
		assertThat(options.getPhase()).isEqualTo(42);
		assertThat(options.getMetricsNamespace()).isEqualTo("my-namespace");
		assertThat(options.getConsumerArn()).isEqualTo("arn:aws:kinesis:consumer");
		assertThat(options.getConsumerName()).isEqualTo("my-consumer");
	}

	@Test
	@DisplayName("toBuilder copies all values and allows selective mutation")
	void toBuilderCopiesAndMutates() {
		KclContainerOptions original = KclContainerOptions.builder().maxRecords(500).autoStartup(false).phase(7)
				.metricsNamespace("ns").workerIdentifier("worker-1").build();

		KclContainerOptions copy = original.toBuilder().maxRecords(999).build();

		assertThat(copy.getMaxRecords()).isEqualTo(999);
		assertThat(copy.isAutoStartup()).isFalse();
		assertThat(copy.getPhase()).isEqualTo(7);
		assertThat(copy.getMetricsNamespace()).isEqualTo("ns");
		assertThat(copy.getWorkerIdentifier()).isEqualTo("worker-1");
		assertThat(original.getMaxRecords()).isEqualTo(500);
	}

}
