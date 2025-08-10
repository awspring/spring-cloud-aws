package io.awspring.cloud.sqs.support.filter;

import org.springframework.messaging.Message;

import java.util.Collection;

public interface MessageFilter<T> {

	Collection<Message<T>> process(Collection<Message<T>> message);
}
