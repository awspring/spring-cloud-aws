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

import org.springframework.core.NestedRuntimeException;

/**
 * Top-level exception for Sqs {@link RuntimeException} instances.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsException extends NestedRuntimeException {

	/**
	 * Construct an instance with the supplied message.
	 * @param msg the message.
	 */
	public SqsException(String msg) {
		super(msg);
	}

	/**
	 * Construct an instance with the supplied message and cause.
	 * @param msg the message.
	 * @param cause the cause.
	 */
	public SqsException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
