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
package io.awspring.cloud.sqs;

import java.util.Collection;

/**
 * Methods related to FIFO queues.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class FifoUtils {

	/**
	 * Prevent instantiation.
	 */
	private FifoUtils() {
	}

	/**
	 * Return whether the provided queue is Fifo.
	 * @param queue the queue.
	 * @return true if fifo.
	 */
	public static boolean isFifo(String queue) {
		return queue.endsWith(".fifo");
	}

	/**
	 * Return whether all provided queues are Fifo.
	 * @param queues the queues.
	 * @return true if all queues are Fifo.
	 */
	public static boolean areAllFifo(Collection<String> queues) {
		return queues.stream().allMatch(FifoUtils::isFifo);
	}

	/**
	 * Return whether all provided queues are not Fifo.
	 * @param queues the queues.
	 * @return true if all queues are not Fifo.
	 */
	public static boolean areNotFifo(Collection<String> queues) {
		return queues.stream().noneMatch(FifoUtils::isFifo);
	}

}
