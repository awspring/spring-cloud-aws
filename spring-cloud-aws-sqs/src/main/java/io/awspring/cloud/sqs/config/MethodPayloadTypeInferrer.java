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

import java.lang.reflect.Method;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Strategy interface for inferring the payload type from a listener method signature. Implementations analyze the
 * method parameters and argument resolvers to dynamically determine which parameter represents the message payload and
 * what its type is.
 *
 * @author Tomaz Fernandes
 * @since 3.4.3
 */
@FunctionalInterface
public interface MethodPayloadTypeInferrer {

	/**
	 * Infer the payload class from the given method and its argument resolvers.
	 * @param method the listener method
	 * @param argumentResolvers the argument resolvers available for this method, may be null or empty
	 * @return the inferred payload class, or null if it cannot be determined
	 */
	@Nullable
	Class<?> inferPayloadType(Method method, @Nullable List<HandlerMethodArgumentResolver> argumentResolvers);

}
