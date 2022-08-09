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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.SqsException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.messaging.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsAcknowledgementException extends SqsException {

	private final Collection<Message<?>> failedAcknowledgements;

	private final String queueUrl;

	public <T> SqsAcknowledgementException(String errorMessage, Collection<Message<T>> failedAcknowledgements,
			String queueUrl, Throwable e) {
		super(errorMessage, e);
		this.queueUrl = queueUrl;
		this.failedAcknowledgements = failedAcknowledgements.stream().map(msg -> (Message<?>) msg)
				.collect(Collectors.toList());
	}

	public Collection<Message<?>> getFailedAcknowledgements() {
		return this.failedAcknowledgements;
	}

	public String getQueueUrl() {
		return this.queueUrl;
	}

}
