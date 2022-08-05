package io.awspring.cloud.sqs.listener.adapter;

import io.awspring.cloud.sqs.listener.MessageListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

import java.util.Collection;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class BlockingMessagingMessageListenerAdapter<T> extends MessagingMessageListenerAdapter<T> implements MessageListener<T> {

	public BlockingMessagingMessageListenerAdapter(InvocableHandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	@Override
	public void onMessage(Message<T> message) {
		super.invokeHandler(message);
	}

	@Override
	public void onMessage(Collection<Message<T>> messages) {
		super.invokeHandler(messages);
	}
}
