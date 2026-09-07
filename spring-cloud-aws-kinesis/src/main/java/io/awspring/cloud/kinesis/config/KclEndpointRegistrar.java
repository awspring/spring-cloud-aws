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
package io.awspring.cloud.kinesis.config;

import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class KclEndpointRegistrar {

	@Nullable
	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	@Nullable
	private Validator validator;

	private Consumer<List<MessageConverter>> messageConvertersConsumer = converters -> {
	};

	private Consumer<List<HandlerMethodArgumentResolver>> methodArgumentResolversConsumer = resolvers -> {
	};

	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		Assert.notNull(messageHandlerMethodFactory, "messageHandlerMethodFactory must not be null");
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	@Nullable
	public MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
		return this.messageHandlerMethodFactory;
	}

	public void setValidator(Validator validator) {
		Assert.notNull(validator, "validator must not be null");
		this.validator = validator;
	}

	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	public void manageMessageConverters(Consumer<List<MessageConverter>> messageConvertersConsumer) {
		Assert.notNull(messageConvertersConsumer, "messageConvertersConsumer must not be null");
		this.messageConvertersConsumer = messageConvertersConsumer;
	}

	public Consumer<List<MessageConverter>> getMessageConvertersConsumer() {
		return this.messageConvertersConsumer;
	}

	public void manageMethodArgumentResolvers(
			Consumer<List<HandlerMethodArgumentResolver>> methodArgumentResolversConsumer) {
		Assert.notNull(methodArgumentResolversConsumer, "methodArgumentResolversConsumer must not be null");
		this.methodArgumentResolversConsumer = methodArgumentResolversConsumer;
	}

	public Consumer<List<HandlerMethodArgumentResolver>> getMethodArgumentResolversConsumer() {
		return this.methodArgumentResolversConsumer;
	}

}
