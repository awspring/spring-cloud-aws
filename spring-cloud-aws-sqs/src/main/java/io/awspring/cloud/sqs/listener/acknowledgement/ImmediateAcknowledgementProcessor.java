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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ImmediateAcknowledgementProcessor<T> extends AbstractOrderingAcknowledgementProcessor<T> {

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Message<T> message) {
		return sendToExecutor(Collections.singletonList(message));
	}

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages) {
		return sendToExecutor(messages);
	}

}
