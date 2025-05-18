package io.awspring.cloud.sqs.support.filter;

import org.springframework.messaging.Message;

public class DefaultMessageFilter<T> implements MessageFilter<T> {
    @Override
    public boolean process(Message<T> message) {
        return true;
    }
}
