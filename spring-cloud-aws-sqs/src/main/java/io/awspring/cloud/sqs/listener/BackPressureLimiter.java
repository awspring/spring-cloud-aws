/*
 * Copyright 2013-2024 the original author or authors.
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
 * The BackPressureLimiter enables a dynamic reduction of the queues consumption capacity depending on external factors.
 */
public interface BackPressureLimiter {

	/**
	 * {@return the limit to be applied to the queue consumption.}
	 *
	 * The limit can be used to reduce the queue consumption capabilities of the next polling attempts. The container
	 * will work toward satisfying the limit by decreasing the maximum number of concurrent messages that can ve
	 * processed.
	 *
	 * The following values will have the following effects:
	 *
	 * <ul>
	 * <li>zero or negative limits will stop consumption from the queue. When such a situation occurs, the queue
	 * processing is said to be on "standby".</li>
	 * <li>Values >= 1 and < {@link ContainerOptions#getMaxConcurrentMessages()} will reduce the queue consumption
	 * capabilities of the next polling attempts.</li>
	 * <li>Values >= {@link ContainerOptions#getMaxConcurrentMessages()} will not reduce the queue consumption
	 * capabilities</li>
	 * </ul>
	 *
	 * Note: the adjustment will require a few polling cycles to be in effect.
	 */
	int limit();
}
