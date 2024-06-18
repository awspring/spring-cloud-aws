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
 * Grouping strategy for Fifo SQS with batch listener mode.
 *
 * @author Alexis SEGURA
 * @since 3.1.2
 */
public enum FifoBatchGroupingStrategy {

	/**
	 * Group messages by message group. Each batch contains messages from a single message group.
	 */
	PROCESS_MESSAGE_GROUPS_IN_PARALLEL_BATCHES,

	/**
	 * Each batch contains messages originating from all the message groups.
	 */
	PROCESS_ALL_GROUPS_IN_EACH_BATCH
}
