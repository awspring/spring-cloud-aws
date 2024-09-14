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
package io.awspring.cloud.docker.compose;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalStackEnvironmentTest {

	@Test
	void getRegionWhenRegionIsNotSet() {
		var environment = new LocalStackEnvironment(Collections.emptyMap());
		assertThat(environment.getRegion()).isNull();
	}

	@Test
	void getRegionWhenRegionIsSet() {
		var environment = new LocalStackEnvironment(Map.of("AWS_DEFAULT_REGION", "us-west-1"));
		assertThat(environment.getRegion()).isEqualTo("us-west-1");
	}

	@Test
	void getAccessKeyWhenAccessKeyIsNotSet() {
		var environment = new LocalStackEnvironment(Collections.emptyMap());
		assertThat(environment.getAccessKey()).isNull();
	}

	@Test
	void getAccessKeyWhenAccessKeyIsSet() {
		var environment = new LocalStackEnvironment(Map.of("AWS_ACCESS_KEY_ID", "access-key"));
		assertThat(environment.getAccessKey()).isEqualTo("access-key");
	}

	@Test
	void getSecretKeyWhenSecretKeyIsNotSet() {
		var environment = new LocalStackEnvironment(Collections.emptyMap());
		assertThat(environment.getSecretKey()).isNull();
	}

	@Test
	void getSecretKeyWhenSecretKeyIsSet() {
		var environment = new LocalStackEnvironment(Map.of("AWS_SECRET_ACCESS_KEY", "secret-key"));
		assertThat(environment.getSecretKey()).isEqualTo("secret-key");
	}
}
