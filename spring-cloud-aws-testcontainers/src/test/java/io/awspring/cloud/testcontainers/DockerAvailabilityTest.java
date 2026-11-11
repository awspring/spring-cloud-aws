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
package io.awspring.cloud.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.DockerClientFactory;

/**
 * Fails the CI build when Testcontainers cannot reach Docker.
 * <p>
 * Test classes annotated with {@code @Testcontainers(disabledWithoutDocker = true)} are skipped when Docker is
 * unavailable, and a build with skipped tests still succeeds. This test uses the same detection and runs only when the
 * {@code CI} environment variable is set, so local builds without Docker keep working.
 *
 * @author Tomaz Fernandes
 */
class DockerAvailabilityTest {

	@Test
	@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
	void dockerIsAvailableInCi() {
		assertThat(DockerClientFactory.instance().isDockerAvailable())
				.as("Docker must be available in CI. Without it, test classes using "
						+ "@Testcontainers(disabledWithoutDocker = true) are skipped and the build still succeeds.")
				.isTrue();
	}

}
