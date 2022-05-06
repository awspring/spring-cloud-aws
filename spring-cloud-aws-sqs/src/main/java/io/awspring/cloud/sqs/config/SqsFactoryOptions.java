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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.messaging.support.config.AbstractFactoryOptions;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsFactoryOptions extends AbstractFactoryOptions<String, SqsFactoryOptions> {

	private Integer simultaneousPollsPerQueue;

	private Integer pollTimeoutSeconds;

	private Integer messagesPerPoll;

	private Integer minTimeToProcess;

	private SqsFactoryOptions() {
	}

	public static SqsFactoryOptions withOptions() {
		return new SqsFactoryOptions();
	}

	public SqsFactoryOptions concurrentPollsPerContainer(int maxActivePollingRequestsPerQueue) {
		this.simultaneousPollsPerQueue = maxActivePollingRequestsPerQueue;
		return this;
	}

	public SqsFactoryOptions pollingTimeoutSeconds(int pollingTimeoutSeconds) {
		this.pollTimeoutSeconds = pollingTimeoutSeconds;
		return this;
	}

	public SqsFactoryOptions messagesPerPoll(int messagesPerPoll) {
		this.messagesPerPoll = messagesPerPoll;
		return this;
	}

	public SqsFactoryOptions minTimeToProcess(int minTimeToProcess) {
		this.minTimeToProcess = minTimeToProcess;
		return this;
	}

	Integer getMinTimeToProcess() {
		return this.minTimeToProcess;
	}

	Integer getSimultaneousPollsPerQueue() {
		return this.simultaneousPollsPerQueue;
	}

	Integer getPollTimeoutSeconds() {
		return this.pollTimeoutSeconds;
	}

	Integer getMessagesPerPoll() {
		return this.messagesPerPoll;
	}

}
