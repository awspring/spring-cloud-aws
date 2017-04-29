/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.messaging.core.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.CachingDestinationResolverProxy;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.core.MessagePostProcessor;

import java.util.Map;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public abstract class AbstractMessageChannelMessagingSendingTemplate<D extends MessageChannel> extends AbstractMessageSendingTemplate<D> implements DestinationResolvingMessageSendingOperations<D> {

    private final DestinationResolver<String> destinationResolver;

    protected AbstractMessageChannelMessagingSendingTemplate(DestinationResolver<String> destinationResolver) {
        this.destinationResolver = new CachingDestinationResolverProxy<>(destinationResolver);
    }

    public void setDefaultDestinationName(String defaultDestination) {
        super.setDefaultDestination(resolveMessageChannelByLogicalName(defaultDestination));
    }

    @Override
    protected void doSend(D destination, Message<?> message) {
        destination.send(message);
    }

    @Override
    public void send(String destinationName, Message<?> message) throws MessagingException {
        D channel = resolveMessageChannelByLogicalName(destinationName);
        doSend(channel, message);
    }

    @Override
    public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
        D channel = resolveMessageChannelByLogicalName(destinationName);
        convertAndSend(channel, payload);
    }

    @Override
    public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers) throws MessagingException {
        D channel = resolveMessageChannelByLogicalName(destinationName);
        convertAndSend(channel, payload, headers);
    }

    @Override
    public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor) throws MessagingException {
        D channel = resolveMessageChannelByLogicalName(destinationName);
        convertAndSend(channel, payload, postProcessor);
    }

    @Override
    public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
        D channel = resolveMessageChannelByLogicalName(destinationName);
        convertAndSend(channel, payload, headers, postProcessor);
    }

    protected D resolveMessageChannelByLogicalName(String destination) {
        String physicalResourceId = this.destinationResolver.resolveDestination(destination);
        return resolveMessageChannel(physicalResourceId);
    }

    protected abstract D resolveMessageChannel(String physicalResourceIdentifier);
}
