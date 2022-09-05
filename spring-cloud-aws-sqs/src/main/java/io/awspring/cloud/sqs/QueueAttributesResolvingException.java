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
package io.awspring.cloud.sqs;

/**
 * Exception thrown when a {@link io.awspring.cloud.sqs.listener.QueueAttributesResolver} fails.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.QueueNotFoundStrategy
 */
public class QueueAttributesResolvingException extends RuntimeException {

	/**
	 * Create an instance with the message and throwable cause.
	 * @param message the error message.
	 * @param cause the cause.
	 */
	public QueueAttributesResolvingException(String message, Throwable cause) {
		super(message, cause);
	}
}
