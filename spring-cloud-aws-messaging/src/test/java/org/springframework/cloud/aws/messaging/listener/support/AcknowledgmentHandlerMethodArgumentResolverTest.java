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

package org.springframework.cloud.aws.messaging.listener.support;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Alain Sahli
 * @since 1.1
 */
public class AcknowledgmentHandlerMethodArgumentResolverTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void resolveArgument_messageWithNoAcknowledgmentHeader_throwIllegalArgumentException()
			throws Exception {
		// Arrange
		AcknowledgmentHandlerMethodArgumentResolver acknowledgmentHandlerMethodArgumentResolver = null;
		acknowledgmentHandlerMethodArgumentResolver = new AcknowledgmentHandlerMethodArgumentResolver(
				"Acknowledgment");
		Message<String> message = MessageBuilder.withPayload("no content").build();

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Acknowledgment");

		// Act
		acknowledgmentHandlerMethodArgumentResolver.resolveArgument(null, message);
	}

}
