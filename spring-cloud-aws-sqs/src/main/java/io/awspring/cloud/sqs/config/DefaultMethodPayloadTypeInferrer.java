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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.support.resolver.BatchPayloadMethodArgumentResolver;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Default implementation of {@link MethodPayloadTypeInferrer} that infers the payload type by analyzing method
 * parameters and their associated argument resolvers.
 * <p>
 * The inference strategy:
 * <ol>
 * <li>If a parameter is explicitly annotated with {@link Payload}, it is considered the payload</li>
 * <li>Otherwise, the first parameter that is not supported by any non-payload resolver is considered the payload</li>
 * </ol>
 * <p>
 * Non-payload resolvers are those that handle framework-specific types like acknowledgements, headers, visibility, or
 * user-provided resolvers for custom types. Payload resolvers handle the actual message payload.
 *
 * @author Tomaz Fernandes
 * @since 3.4.3
 */
public class DefaultMethodPayloadTypeInferrer implements MethodPayloadTypeInferrer {

	@Override
	@Nullable
	public Class<?> inferPayloadType(Method method, List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null || argumentResolvers.isEmpty()) {
			return null;
		}

		List<HandlerMethodArgumentResolver> nonPayloadResolvers = argumentResolvers.stream()
				.filter(resolver -> !isPayloadResolver(resolver)).toList();

		for (int i = 0; i < method.getParameterCount(); i++) {
			MethodParameter parameter = new MethodParameter(method, i);

			if (parameter.hasParameterAnnotation(Payload.class)) {
				return extractClass(parameter.getGenericParameterType());
			}

			boolean supportedByNonPayloadResolver = nonPayloadResolvers.stream()
					.anyMatch(resolver -> resolver.supportsParameter(parameter));

			if (!supportedByNonPayloadResolver) {
				return extractClass(parameter.getGenericParameterType());
			}
		}

		return null;
	}

	/**
	 * Extract the target class for payload conversion from the inferred type. Handles generic types like
	 * {@code List<CustomEvent>} by extracting the element type.
	 * @param type the inferred payload type
	 * @return the class to be used for payload conversion, or null if cannot be determined
	 */
	@Nullable
	private Class<?> extractClass(Type type) {
		ResolvableType resolvableType = ResolvableType.forType(type);
		Class<?> rawClass = resolvableType.toClass();

		// If it's a Collection (e.g., List<CustomEvent>), extract the element type
		if (Collection.class.isAssignableFrom(rawClass)) {
			Class<?> elementClass = resolvableType.getNested(2).toClass();
			// If it's a Collection of Messages (e.g., List<Message<CustomEvent>>), go one level deeper
			if (Message.class.isAssignableFrom(elementClass)) {
				return resolvableType.getNested(3).toClass();
			}
			return elementClass;
		}

		// If it's a Message<T>, unwrap to get T
		if (Message.class.isAssignableFrom(rawClass)) {
			return resolvableType.getNested(2).toClass();
		}

		// For simple types, return as-is
		return rawClass;
	}

	private boolean isPayloadResolver(HandlerMethodArgumentResolver resolver) {
		return resolver instanceof PayloadMethodArgumentResolver
				|| resolver instanceof BatchPayloadMethodArgumentResolver
				|| resolver instanceof MessageMethodArgumentResolver;
	}

}
