package io.awspring.cloud.sqs.support.filter;

import org.springframework.messaging.Message;

import java.util.Collection;

public class DefaultMessageFilter<T> implements MessageFilter<T> {

    @Override
	public Collection<Message<T>> process(Collection<Message<T>> message) {
        return message;
    }
}
