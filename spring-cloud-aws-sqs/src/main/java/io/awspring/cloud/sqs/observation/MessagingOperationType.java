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
package io.awspring.cloud.sqs.observation;

/**
 * @author Mariusz Sondecki
 */
public enum MessagingOperationType {
	// @formatter:off
	SINGLE_MANUAL_PROCESS("single message manual process"),
	SINGLE_POLLING_PROCESS("single message polling process"),
	BATCH_MANUAL_PROCESS("batch message manual process"),
	BATCH_POLLING_PROCESS("batch message polling process"),
	SINGLE_PUBLISH("single message publish"),
	BATCH_PUBLISH("batch message publish");
	// @formatter:on

	private final String value;

	MessagingOperationType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
