/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core.support;

import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public abstract class AbstractMessageChannelMessagingSendingTemplate<T extends MessageChannel> extends AbstractMessageSendingTemplate<String> {

	private final DestinationResolver<String> destinationResolver;

	protected AbstractMessageChannelMessagingSendingTemplate(DestinationResolver<String> destinationResolver) {
		this.destinationResolver = new CachingDestinationResolver<String>(destinationResolver);
	}

	@Override
	protected void doSend(String destination, Message<?> message) {
		resolveMessageChannelByLogicalName(destination).send(message);
	}

	protected T resolveMessageChannelByLogicalName(String destination) {
		String physicalResourceId = this.destinationResolver.resolveDestination(destination);
		return resolveMessageChannel(physicalResourceId);
	}

	protected abstract T resolveMessageChannel(String physicalResourceIdentifier);
}