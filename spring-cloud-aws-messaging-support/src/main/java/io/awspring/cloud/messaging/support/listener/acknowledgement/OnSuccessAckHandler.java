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
package io.awspring.cloud.messaging.support.listener.acknowledgement;

import io.awspring.cloud.messaging.support.listener.MessageHeaders;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class OnSuccessAckHandler<T> implements AsyncAckHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(OnSuccessAckHandler.class);

	@SuppressWarnings("unchecked")
	@Override
	public CompletableFuture<Void> onSuccess(Message<T> message) {
		logger.trace("Acknowledging message " + message);
		Object ackObject = message.getHeaders().get(MessageHeaders.ACKNOWLEDGMENT_HEADER);
		Assert.notNull(ackObject, () -> "No acknowledgment found for " + message);
		Assert.isInstanceOf(AsyncAcknowledgement.class, ackObject, () -> "Wrong ack type for message:  " + ackObject);
		return ((AsyncAcknowledgement) ackObject).acknowledge();
	}
}
