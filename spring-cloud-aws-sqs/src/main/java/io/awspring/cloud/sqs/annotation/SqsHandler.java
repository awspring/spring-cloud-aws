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
package io.awspring.cloud.sqs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * Methods that are from classes annotated with {@link SqsListener} and are annotated with {@link SqsHandler} will be
 * marked as the target of the SQS message listener based on the message payload type.
 *
 * <p>
 * Each payload type must have exactly one corresponding method.
 * <p>
 * If no method matches the payload type, a method marked as the default (using {@code isDefault = true}) will be
 * invoked. Only one method can be designated as the default.
 *
 * @author José Iêdo
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MessageMapping
public @interface SqsHandler {

	/**
	 * Indicates whether this method should be used as the default fallback method if no other {@link SqsHandler} method
	 * matches the payload type.
	 *
	 * @return {@code true} if this is the default method, {@code false} otherwise
	 */
	boolean isDefault() default false;
}
