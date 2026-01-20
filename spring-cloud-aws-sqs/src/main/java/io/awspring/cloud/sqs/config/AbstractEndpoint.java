/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.listener.AbstractMessageListenerContainer;
import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.ListenerMode;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement;
import io.awspring.cloud.sqs.listener.adapter.AsyncMessagingMessageListenerAdapter;
import io.awspring.cloud.sqs.listener.adapter.MessagingMessageListenerAdapter;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;

/**
 * Base class for implementing a {@link HandlerMethodEndpoint}.
 *
 * @author Tomaz Fernandes
 * @author José Iêdo
 * @since 3.0
 */
public abstract class AbstractEndpoint implements HandlerMethodEndpoint {

	private final Collection<String> logicalNames;

	private final String listenerContainerFactoryName;

	private final String id;

	private Object bean;

	private Method method;

	private MessageHandlerMethodFactory handlerMethodFactory;

	@Nullable
	private MethodPayloadTypeInferrer methodPayloadTypeInferrer;

	@Nullable
	private List<HandlerMethodArgumentResolver> argumentResolvers;

	protected AbstractEndpoint(Collection<String> queueNames, @Nullable String listenerContainerFactoryName,
			String id) {
		Assert.notEmpty(queueNames, "queueNames cannot be empty.");
		this.id = id;
		this.logicalNames = queueNames;
		this.listenerContainerFactoryName = listenerContainerFactoryName;
	}

	@Override
	public Collection<String> getLogicalNames() {
		return this.logicalNames;
	}

	@Override
	public String getListenerContainerFactoryName() {
		return this.listenerContainerFactoryName;
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * Set the bean instance to be used when handling a message for this endpoint.
	 * @param bean the bean instance.
	 */
	@Override
	public void setBean(Object bean) {
		this.bean = bean;
	}

	/**
	 * Get the bean instance to be used when handling a message for this endpoint.
	 * @return the bean instance.
	 */
	public Object getBean() {
		return this.bean;
	}

	/**
	 * Set the method to be used when handling a message for this endpoint.
	 * @param method the method.
	 */
	@Override
	public void setMethod(Method method) {
		this.method = method;
	}

	/**
	 * Set the {@link MessageHandlerMethodFactory} to be used for handling messages in this endpoint.
	 * @param handlerMethodFactory the factory.
	 */
	@Override
	public void setHandlerMethodFactory(MessageHandlerMethodFactory handlerMethodFactory) {
		Assert.notNull(handlerMethodFactory, "handlerMethodFactory cannot be null");
		this.handlerMethodFactory = handlerMethodFactory;
	}

	/**
	 * Get the {@link MessageHandlerMethodFactory} to be used for handling messages in this endpoint.
	 * @return the factory.
	 */
	@Nullable
	public MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
		return this.handlerMethodFactory;
	}

	/**
	 * Set the {@link MethodPayloadTypeInferrer} to be used for inferring payload types from method signatures.
	 *
	 * @param inferrer the inferrer instance, or null to disable inference.
	 */
	@Override
	public void setMethodPayloadTypeInferrer(@Nullable MethodPayloadTypeInferrer inferrer) {
		this.methodPayloadTypeInferrer = inferrer;
	}

	/**
	 * Set the argument resolvers to be used for inferring payload types if a methodPayloadTypeInferrer is set.
	 *
	 * @param argumentResolvers the argument resolvers, may be null.
	 */
	@Override
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	@Override
	public void configureListenerMode(Consumer<ListenerMode> consumer) {
		List<MethodParameter> parameters = getMethodParameters();
		boolean batch = hasParameterOfType(parameters, List.class);
		boolean batchAckParameter = hasParameterOfType(parameters, BatchAcknowledgement.class);
		boolean batchVisibilityParameter = hasParameterOfType(parameters, BatchVisibility.class);
		Assert.isTrue(hasValidParameters(batch, batchAckParameter, batchVisibilityParameter, parameters.size()),
				getInvalidParametersMessage());
		consumer.accept(batch ? ListenerMode.BATCH : ListenerMode.SINGLE_MESSAGE);
	}

	private boolean hasValidParameters(boolean batch, boolean batchAckParameter, boolean batchVisibilityParameter,
			int size) {
		return hasValidSingleMessageParameters(batch, batchAckParameter, batchVisibilityParameter)
				|| hasValidBatchParameters(batchAckParameter, batchVisibilityParameter, size);
	}

	private boolean hasValidSingleMessageParameters(boolean batch, boolean batchAckParameter,
			boolean batchVisibilityParameter) {
		return !batch && !batchAckParameter && !batchVisibilityParameter;
	}

	private boolean hasValidBatchParameters(boolean batchAckParameter, boolean batchVisibilityParameter, int size) {
		long expectedAdditionalParams = Stream.of(batchAckParameter, batchVisibilityParameter).filter(b -> b).count();
		return size == expectedAdditionalParams + 1;
	}

	private String getInvalidParametersMessage() {
		return String.format(
				"Method %s from class %s in endpoint %s has invalid parameters for batch processing. "
						+ "Batch methods must have a single List parameter, either of Message<T> or T types,"
						+ "and optionally BatchAcknowledgement and BatchVisibility parameters.",
				this.method.getName(), this.method.getDeclaringClass(), this.id);
	}

	private boolean hasParameterOfType(List<MethodParameter> parameters, Class<?> clazz) {
		return parameters.stream().anyMatch(param -> clazz.isAssignableFrom(param.getParameterType()));
	}

	private List<MethodParameter> getMethodParameters() {
		return IntStream.range(0, BridgeMethodResolver.findBridgedMethod(this.method).getParameterCount())
				.mapToObj(index -> new MethodParameter(this.method, index)).collect(Collectors.toList());
	}

	/**
	 * Configure the provided container for this endpoint.
	 * @param container the container to be configured.
	 */
	public <T> void setupContainer(MessageListenerContainer<T> container) {
		Assert.notNull(this.handlerMethodFactory, "No handlerMethodFactory has been set");
		InvocableHandlerMethod handlerMethod = this.handlerMethodFactory.createInvocableHandlerMethod(this.bean,
				this.method);

		if (this.methodPayloadTypeInferrer != null
				&& container instanceof AbstractMessageListenerContainer<?, ?, ?> amlc) {
			Class<?> inferredType = this.methodPayloadTypeInferrer.inferPayloadType(this.method,
					this.argumentResolvers);
			if (inferredType != null) {
				amlc.setPayloadDeserializationType(inferredType);
			}
			disableDefaultPayloadTypeMapper(amlc);
		}

		if (CompletionStage.class.isAssignableFrom(handlerMethod.getReturnType().getParameterType())) {
			container.setAsyncMessageListener(createAsyncMessageListenerInstance(handlerMethod));
		}
		else {
			container.setMessageListener(createMessageListenerInstance(handlerMethod));
		}
	}

	private void disableDefaultPayloadTypeMapper(AbstractMessageListenerContainer<?, ?, ?> container) {
		var converter = container.getContainerOptions().getMessageConverter();
		if (converter instanceof AbstractMessagingMessageConverter<?> amc && amc.isUsingDefaultPayloadTypeMapper()) {
			// Disable JavaType header based mapping
			amc.setPayloadTypeMapper(msg -> null);
		}
	}

	protected <T> MessageListener<T> createMessageListenerInstance(InvocableHandlerMethod handlerMethod) {
		return new MessagingMessageListenerAdapter<>(handlerMethod);
	}

	protected <T> AsyncMessageListener<T> createAsyncMessageListenerInstance(InvocableHandlerMethod handlerMethod) {
		return new AsyncMessagingMessageListenerAdapter<>(handlerMethod);
	}

}
