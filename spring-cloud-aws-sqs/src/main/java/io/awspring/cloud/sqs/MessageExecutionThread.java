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

import org.jspecify.annotations.Nullable;

/**
 * A {@link Thread} implementation for processing messages.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see MessageExecutionThreadFactory
 * @see io.awspring.cloud.sqs.listener.AsyncComponentAdapters
 */
public class MessageExecutionThread extends Thread {

	/**
	 * Create an instance with the provided arguments. See {@link Thread} javadoc.
	 * @param threadGroup see {@link Thread} javadoc.
	 * @param runnable see {@link Thread} javadoc.
	 * @param nextThreadName see {@link Thread} javadoc.
	 */
	public MessageExecutionThread(@Nullable ThreadGroup threadGroup, Runnable runnable, String nextThreadName) {
		super(threadGroup, runnable, nextThreadName);
	}

	/**
	 * Create an instance. See {@link Thread} javadoc.
	 */
	public MessageExecutionThread() {
		super();
	}
}
