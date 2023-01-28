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
 * Configuration for application throughput.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public enum BackPressureMode {

	/**
	 * Enable automatic throughput switching and partial batch polling.
	 * <p>
	 * Starts in a LOW throughput mode where only one poll is made at a time. When a message is received, switches to
	 * HIGH throughput mode. If a poll returns empty and there are no inflight messages, switches back to LOW throughput
	 * mode, and so forth.
	 * <p>
	 * If the current number of inflight messages is close to {@link ContainerOptions#getMaxConcurrentMessages()}, the
	 * framework will try to acquire a partial batch with the remaining value.
	 * <p>
	 * This is the default setting and should be balanced for most applications.
	 */
	AUTO,

	/**
	 * Enable automatic throughput switching and disable partial batch polling.
	 * <p>
	 * If the current number of inflight messages is close to {@link ContainerOptions#getMaxConcurrentMessages()}, the
	 * framework will wait until a full batch can be polled.
	 * <p>
	 * Useful for scenarios where the cost of retrieving less messages in a poll, and consequentially making more polls,
	 * is higher than the cost of waiting for more messages to be processed.
	 */
	ALWAYS_POLL_MAX_MESSAGES,

	/**
	 * Set fixed high throughput mode. In this mode up to (maxConcurrentMessages / messagesPerPoll) simultaneous polls
	 * will be made until maxConcurrentMessages is achieved.
	 * <p>
	 * Useful for really high-throughput scenarios where the occasional automatic switch to a lower throughput would be
	 * costly.
	 */
	FIXED_HIGH_THROUGHPUT

}
