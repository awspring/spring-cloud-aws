/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

/**
 * A callback for target record process to perform checkpoint on the related shard.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public interface Checkpointer {

	/**
	 * Checkpoint the currently held sequence number if it is bigger than already stored.
	 * @return true if checkpoint performed; false otherwise.
	 */
	boolean checkpoint();

	/**
	 * Checkpoint the provided sequence number, if it is bigger than already stored.
	 * @param sequenceNumber the sequence number to checkpoint.
	 * @return true if checkpoint performed; false otherwise.
	 */
	boolean checkpoint(String sequenceNumber);

}
