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
package io.awspring.cloud.sns.core;

import io.awspring.cloud.core.support.JacksonPresent;

/**
 * Depending on dependencies present configures {@link JacksonJsonStringEncoder} or {@link Jackson2JsonStringEncoder}
 * @author Matej Nedic
 * @since 4.0.0
 */
public interface JsonStringEncoder {
	static JsonStringEncoder create() {
		if (JacksonPresent.isJackson3Present()) {
			return new JacksonJsonStringEncoder();
		}
		else if (JacksonPresent.isJackson2Present()) {
			return new Jackson2JsonStringEncoder();
		}
		else {
			throw new IllegalStateException(
					"JsonStringEncoder requires a Jackson 2 or Jackson 3 library on the classpath");
		}
	}

	void quoteAsString(CharSequence input, StringBuilder output);
}
