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
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class QueueAttributes {

	private final String destinationUrl;

	private final boolean hasRedrivePolicy;

	private final Integer visibilityTimeout;

	private final boolean fifo;

	public QueueAttributes(String destinationUrl, boolean hasRedrivePolicy, Integer visibilityTimeout, boolean fifo) {
		this.hasRedrivePolicy = hasRedrivePolicy;
		this.destinationUrl = destinationUrl;
		this.visibilityTimeout = visibilityTimeout;
		this.fifo = fifo;
	}

	public boolean hasRedrivePolicy() {
		return this.hasRedrivePolicy;
	}

	boolean isFifo() {
		return fifo;
	}

	public String getDestinationUrl() {
		return destinationUrl;
	}

	public Integer getVisibilityTimeout() {
		return visibilityTimeout;
	}
}
