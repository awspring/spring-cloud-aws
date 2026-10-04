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

import java.lang.reflect.Method;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * {@link KclEndpoint} specialization whose records are handled by a
 * {@link org.springframework.messaging.handler.HandlerMethod}, as is the case for
 * {@link io.awspring.cloud.kinesis.annotation.KclListener} annotated methods.
 * <p>
 * Endpoints that dispatch to a {@link io.awspring.cloud.kinesis.listener.MessageListener} directly can implement
 * {@link KclEndpoint} instead.
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
public interface KclHandlerMethodEndpoint extends KclEndpoint {

	/**
	 * The bean containing the method to invoke with the incoming records.
	 * @return the bean.
	 */
	Object getBean();

	/**
	 * The method to invoke with the incoming records.
	 * @return the method.
	 */
	Method getMethod();

	/**
	 * Sets the factory creating the {@link org.springframework.messaging.handler.HandlerMethod} for this endpoint.
	 * @param handlerMethodFactory the factory.
	 */
	void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory);

	/**
	 * The factory creating the {@link org.springframework.messaging.handler.HandlerMethod} for this endpoint.
	 * @return the factory.
	 */
	MessageHandlerMethodFactory getHandlerMethodFactory();

}
