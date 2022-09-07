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
package io.awspring.cloud.sns.core;

import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * Helper class used for conversion between {@link MessageAttributeValue} and Java types.
 *
 * @author Alain Sahli
 * @since 1.0
 */
public final class MessageAttributeDataTypes {

	/**
	 * Binary message attribute data type.
	 */
	public static final String BINARY = "Binary";

	/**
	 * Number message attribute data type.
	 */
	public static final String NUMBER = "Number";

	/**
	 * String message attribute data type.
	 */
	public static final String STRING = "String";

	/**
	 * String.Array message attribute data type.
	 */
	public static final String STRING_ARRAY = "String.Array";

	private MessageAttributeDataTypes() {
		// Avoid instantiation
	}

}
