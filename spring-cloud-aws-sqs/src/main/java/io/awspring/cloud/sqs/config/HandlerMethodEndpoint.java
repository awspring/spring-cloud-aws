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

import io.awspring.cloud.sqs.listener.ListenerMode;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * {@link Endpoint} specialization that indicates that {@link org.springframework.messaging.Message} instances coming
 * from this endpoint will be handled by a {@link org.springframework.messaging.handler.HandlerMethod}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface HandlerMethodEndpoint extends Endpoint {

	/**
	 * Set the bean containing the method to be invoked with the incoming messages.
	 * @param bean the bean.
	 */
	void setBean(Object bean);

	/**
	 * Set the method to be used when handling messages for this endpoint.
	 * @param method the method.
	 */
	void setMethod(Method method);

	/**
	 * Set the {@link MessageHandlerMethodFactory} to be used for creating the
	 * {@link org.springframework.messaging.handler.HandlerMethod}.
	 * @param handlerMethodFactory the factory.
	 */
	void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory);

	/**
	 * Set the {@link MethodPayloadTypeInferrer} to be used for inferring payload types from method signatures.
	 * @param inferrer the inferrer instance, or null to disable inference.
	 */
	void setMethodPayloadTypeInferrer(@Nullable MethodPayloadTypeInferrer inferrer);

	/**
	 * Set the argument resolvers to be used for inferring payload types.
	 * @param argumentResolvers the argument resolvers, may be null.
	 */
	void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers);

	/**
	 * Allows configuring the {@link ListenerMode} for this endpoint.
	 * @param consumer a consumer for the strategy used by this endpoint.
	 */
	void configureListenerMode(Consumer<ListenerMode> consumer);

}
