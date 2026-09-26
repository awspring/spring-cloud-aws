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
package io.awspring.cloud.sqs.listener;

/**
 * Configure the strategy to be used when a specified queue is not found at container startup.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see SqsContainerOptions#getQueueNotFoundStrategy()
 */
public enum QueueNotFoundStrategy {

	/**
	 * Throw an exception and stop application startup if a queue is not found.
	 */
	FAIL,

	/**
	 * Create queues that are not found at startup. Mind that in production environments the application might not have
	 * permissions to create the queue and throw an exception.
	 */
	CREATE,

	/**
	 * Skip starting the message source for a queue that is not found, log a warning, and allow application startup to
	 * proceed. Since the source is not started, no polling thread is created for that queue. The queue will be resolved
	 * again on the next container restart, which allows the listener to start normally if the queue has been created.
	 * Useful when a queue may legitimately be absent in some deployments (for example, optional feature queues) and
	 * neither {@link #CREATE} nor {@link #FAIL} fits. This mirrors the default behavior of Spring Cloud AWS 2.x's
	 * {@code spring-cloud-starter-aws-messaging}, which silently ignored missing queues at startup.
	 */
	IGNORE

}
