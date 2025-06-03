package io.awspring.cloud.sqs.listener.sink;

import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.support.filter.MessageFilter;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FilteredBatchMessageSink<T> extends BatchMessageSink<T> {

    private final MessageFilter<T> filter;

    public FilteredBatchMessageSink(MessageFilter<T> filter) {
        this.filter = filter;
    }

    @Override
    protected CompletableFuture<Void> doEmit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
        List<Message<T>> filtered = new ArrayList<>(messages.size());
        for (Message<T> message : messages) {
            if (filter.process(message)) {
                filtered.add(message);
            }
        }

        if (filtered.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return super.doEmit(filtered, context);
    }
}
