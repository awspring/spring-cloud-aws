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

import io.micrometer.observation.Observation;
import java.util.Collection;
import java.util.Collections;
import org.springframework.messaging.MessageHeaders;

/**
 * Context that holds information for observation metadata collection during the manual process of SQS messages.
 *
 * @author Mariusz Sondecki
 */
public class MessageManualProcessObservationContext extends Observation.Context {

	private Collection<MessageHeaders> messageHeaders = Collections.emptyList();

	public Collection<MessageHeaders> getMessageHeaders() {
		return messageHeaders;
	}

	public void setMessageHeaders(Collection<MessageHeaders> messageHeaders) {
		this.messageHeaders = messageHeaders;
	}
}
