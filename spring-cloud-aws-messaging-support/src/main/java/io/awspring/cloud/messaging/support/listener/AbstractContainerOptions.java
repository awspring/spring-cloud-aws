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
package io.awspring.cloud.messaging.support.listener;

import java.time.Duration;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractContainerOptions<O extends AbstractContainerOptions<?>> implements ContainerOptions {

	private static final int DEFAULT_SIMULTANEOUS_PRODUCE_CALLS = 2;

	private static final int DEFAULT_MESSAGES_PER_PRODUCE = 10;

	private static final int DEFAULT_PRODUCE_TIMEOUT = 10;

	private int simultaneousProduceCalls = DEFAULT_SIMULTANEOUS_PRODUCE_CALLS;

	private int messagesPerProduce = DEFAULT_MESSAGES_PER_PRODUCE;

	private int produceTimeout = DEFAULT_PRODUCE_TIMEOUT;

	@SuppressWarnings("unchecked")
	private O self() {
		return (O) this;
	}

	public O simultaneousProduceCalls(int simultaneousProduceCalls) {
		this.simultaneousProduceCalls = simultaneousProduceCalls;
		return self();
	}

	public O messagesPerProduce(int messagesPerProduce) {
		this.messagesPerProduce = messagesPerProduce;
		return self();
	}

	public O produceTimeout(Integer produceTimeout) {
		this.produceTimeout = produceTimeout;
		return self();
	}

	public int getSimultaneousProduceCalls() {
		return this.simultaneousProduceCalls;
	}

	public int getMessagesPerProduce() {
		return this.messagesPerProduce;
	}

	public Duration getProduceTimeout() {
		return Duration.ofSeconds(this.produceTimeout);
	}

}
