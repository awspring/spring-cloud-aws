/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

/**
 * The strategy to use when handling a send batch operation that has at least one failed message.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public enum SendBatchFailureStrategy {

	/**
	 * Throw a {@link SendBatchOperationFailedException} with a {@link SendResult.Batch} object.
	 */
	THROW_EXCEPTION,

	/**
	 * Do not throw an exception and return the {@link SendResult.Batch} object directly.
	 */
	RETURN_RESULT

}
