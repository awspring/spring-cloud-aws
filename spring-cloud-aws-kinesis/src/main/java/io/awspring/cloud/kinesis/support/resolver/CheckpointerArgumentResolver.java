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
package io.awspring.cloud.kinesis.support.resolver;

import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointer;
import io.awspring.cloud.kinesis.support.converter.KinesisMessageHeaders;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class CheckpointerArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return KclCheckpointer.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		return message.getHeaders().get(KinesisMessageHeaders.CHECKPOINTER, KclCheckpointer.class);
	}

}
