/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.listener.MessageDeliveryStrategy;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface HandlerMethodEndpoint extends Endpoint {

	void setBean(Object bean);

	/**
	 * Set the method to be used when handling a message for this endpoint.
	 * @param method the method.
	 */
	void setMethod(Method method);

	/**
	 * Set the {@link MessageHandlerMethodFactory} to be used for handling messages in this endpoint.
	 * @param handlerMethodFactory the factory.
	 */
	void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory);

	void configureMessageDeliveryStrategy(Consumer<MessageDeliveryStrategy> consumer);

}
