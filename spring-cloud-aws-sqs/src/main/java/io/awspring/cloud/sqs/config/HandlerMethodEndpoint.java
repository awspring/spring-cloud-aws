package io.awspring.cloud.sqs.config;

import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.lang.reflect.Method;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface HandlerMethodEndpoint extends Endpoint {

	void setBean(Object bean);

	/**
	 * Set the method to be used when handling a message for this endpoint.
	 * @param method the method.
	 */
	void setMethod(Method method);

	/**
	 * Set the {@link MessageHandlerMethodFactory} to be used for handling messages in this endpoint.
	 * @param handlerMethodFactory the factory.
	 */
	void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory);

}
