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
 * The ContentBasedDeduplication queue attribute value to be used by the {@link SqsTemplate} when sending messages to a
 * FIFO queue.
 *
 * @author Zhong Xi Lu
 * @since 3.0.4
 */
public enum TemplateContentBasedDeduplication {

	/**
	 * The ContentBasedDeduplication queue attribute value will be resolved automatically at runtime.
	 */
	AUTO,

	/**
	 * ContentBasedDeduplication is enabled on all FIFO SQS queues.
	 */
	ENABLED,

	/**
	 * ContentBasedDeduplication is disabled on all FIFO SQS queues.
	 */
	DISABLED
}
