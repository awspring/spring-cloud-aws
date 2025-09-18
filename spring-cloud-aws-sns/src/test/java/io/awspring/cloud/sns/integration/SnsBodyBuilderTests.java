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
package io.awspring.cloud.sns.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
class SnsBodyBuilderTests {

	@Test
	void snsBodyBuilder() {
		assertThatIllegalArgumentException().isThrownBy(() -> SnsBodyBuilder.withDefault(""))
				.withMessageContaining("defaultMessage must not be empty.");

		String message = SnsBodyBuilder.withDefault("foo").build();
		assertThat(message).isEqualTo("{\"default\":\"foo\"}");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> SnsBodyBuilder.withDefault("foo").forProtocols("{\"foo\" : \"bar\"}").build())
				.withMessageContaining("protocols must not be empty.");

		assertThatIllegalArgumentException()
				.isThrownBy(() -> SnsBodyBuilder.withDefault("foo").forProtocols("{\"foo\" : \"bar\"}", "").build())
				.withMessageContaining("protocols must not contain empty elements.");

		message = SnsBodyBuilder.withDefault("foo").forProtocols("{\"foo\" : \"bar\"}", "sms").build();

		assertThat(message).isEqualTo("{\"default\":\"foo\",\"sms\":\"{\\\"foo\\\" : \\\"bar\\\"}\"}");
	}

}
