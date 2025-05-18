package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.support.filter.MessageFilter;
import org.springframework.messaging.Message;

public class FilteringMessageListenerAdapter<T> implements MessageListener<T> {

	private final AsyncMessageListener<T> delegate;
	private final MessageFilter<T> filter;

	public FilteringMessageListenerAdapter(AsyncMessageListener<T> delegate, MessageFilter<T> filter) {
		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public void onMessage(Message<T> message) {
		if (filter.process(message)) {
			delegate.onMessage(message);
		}
	}
}

