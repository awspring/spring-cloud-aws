package io.awspring.cloud.sqs.support.filter;

import org.springframework.messaging.Message;

public interface MessageFilter<T> {
    boolean process(Message<T> message);
}
