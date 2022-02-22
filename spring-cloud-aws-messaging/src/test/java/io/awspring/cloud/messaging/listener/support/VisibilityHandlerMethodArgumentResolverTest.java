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

package io.awspring.cloud.messaging.listener.support;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Szymon Dembek
 * @since 1.3
 */
class VisibilityHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_messageWithNoVisibilityHeader_throwIllegalArgumentException() throws Exception {
		// Arrange
		VisibilityHandlerMethodArgumentResolver visibilityHandlerMethodArgumentResolver;
		visibilityHandlerMethodArgumentResolver = new VisibilityHandlerMethodArgumentResolver("Visibility");
		Message<String> message = MessageBuilder.withPayload("no content").build();

		// Assert
		assertThatThrownBy(() -> visibilityHandlerMethodArgumentResolver.resolveArgument(null, message))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Visibility");
	}

}
