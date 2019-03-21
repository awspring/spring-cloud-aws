/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.listener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.cloud.aws.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.listener.support.VisibilityHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.support.NotificationMessageArgumentResolver;
import org.springframework.cloud.aws.messaging.support.NotificationSubjectArgumentResolver;
import org.springframework.cloud.aws.messaging.support.converter.ObjectMessageConverter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.ClassUtils;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
public class QueueMessageHandler
		extends AbstractMethodMessageHandler<QueueMessageHandler.MappingInformation> {

	static final String LOGICAL_RESOURCE_ID = "LogicalResourceId";
	static final String ACKNOWLEDGMENT = "Acknowledgment";
	static final String VISIBILITY = "Visibility";

	private final List<MessageConverter> messageConverters;

	public QueueMessageHandler(List<MessageConverter> messageConverters) {
		this.messageConverters = messageConverters;
	}

	public QueueMessageHandler() {
		this.messageConverters = Collections.emptyList();
	}

	private static String[] wrapInStringArray(Object valueToWrap) {
		return new String[] { valueToWrap.toString() };
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(
				getCustomArgumentResolvers());

		resolvers.add(new HeaderMethodArgumentResolver(null, null));
		resolvers.add(new HeadersMethodArgumentResolver());

		resolvers.add(new NotificationSubjectArgumentResolver());
		resolvers.add(new AcknowledgmentHandlerMethodArgumentResolver(ACKNOWLEDGMENT));
		resolvers.add(new VisibilityHandlerMethodArgumentResolver(VISIBILITY));

		CompositeMessageConverter compositeMessageConverter = createPayloadArgumentCompositeConverter();
		resolvers.add(new NotificationMessageArgumentResolver(compositeMessageConverter));
		resolvers.add(new PayloadArgumentResolver(compositeMessageConverter,
				new NoOpValidator()));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {

		return new ArrayList<>(this.getCustomReturnValueHandlers());
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return true;
	}

	@Override
	protected MappingInformation getMappingForMethod(Method method,
			Class<?> handlerType) {
		SqsListener sqsListenerAnnotation = AnnotationUtils.findAnnotation(method,
				SqsListener.class);
		if (sqsListenerAnnotation != null && sqsListenerAnnotation.value().length > 0) {
			if (sqsListenerAnnotation.deletionPolicy() == SqsMessageDeletionPolicy.NEVER
					&& hasNoAcknowledgmentParameter(method.getParameterTypes())) {
				this.logger.warn("Listener method '" + method.getName() + "' in type '"
						+ method.getDeclaringClass().getName()
						+ "' has deletion policy 'NEVER' but does not have a parameter of type Acknowledgment.");
			}
			return new MappingInformation(
					resolveDestinationNames(sqsListenerAnnotation.value()),
					sqsListenerAnnotation.deletionPolicy());
		}

		MessageMapping messageMappingAnnotation = AnnotationUtils.findAnnotation(method,
				MessageMapping.class);
		if (messageMappingAnnotation != null
				&& messageMappingAnnotation.value().length > 0) {
			return new MappingInformation(
					resolveDestinationNames(messageMappingAnnotation.value()),
					SqsMessageDeletionPolicy.ALWAYS);
		}

		return null;
	}

	private boolean hasNoAcknowledgmentParameter(Class<?>[] parameterTypes) {
		for (Class<?> parameterType : parameterTypes) {
			if (ClassUtils.isAssignable(Acknowledgment.class, parameterType)) {
				return false;
			}
		}

		return true;
	}

	private Set<String> resolveDestinationNames(String[] destinationNames) {
		Set<String> result = new HashSet<>(destinationNames.length);

		for (String destinationName : destinationNames) {
			result.addAll(Arrays.asList(resolveName(destinationName)));
		}

		return result;
	}

	private String[] resolveName(String name) {
		if (!(getApplicationContext() instanceof ConfigurableApplicationContext)) {
			return wrapInStringArray(name);
		}

		ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) getApplicationContext();
		ConfigurableBeanFactory configurableBeanFactory = applicationContext
				.getBeanFactory();

		String placeholdersResolved = configurableBeanFactory.resolveEmbeddedValue(name);
		BeanExpressionResolver exprResolver = configurableBeanFactory
				.getBeanExpressionResolver();
		if (exprResolver == null) {
			return wrapInStringArray(name);
		}
		Object result = exprResolver.evaluate(placeholdersResolved,
				new BeanExpressionContext(configurableBeanFactory, null));
		if (result instanceof String[]) {
			return (String[]) result;
		}
		else if (result != null) {
			return wrapInStringArray(result);
		}
		else {
			return wrapInStringArray(name);
		}
	}

	@Override
	protected Set<String> getDirectLookupDestinations(MappingInformation mapping) {
		return mapping.getLogicalResourceIds();
	}

	@Override
	protected String getDestination(Message<?> message) {
		return message.getHeaders().get(LOGICAL_RESOURCE_ID).toString();
	}

	@Override
	protected MappingInformation getMatchingMapping(MappingInformation mapping,
			Message<?> message) {
		if (mapping.getLogicalResourceIds().contains(getDestination(message))) {
			return mapping;
		}
		else {
			return null;
		}
	}

	@Override
	protected Comparator<MappingInformation> getMappingComparator(Message<?> message) {
		return new ComparableComparator<>();
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(
			Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

	@Override
	protected void handleNoMatch(Set<MappingInformation> ts, String lookupDestination,
			Message<?> message) {
		this.logger.warn("No match found");
	}

	@Override
	protected void processHandlerMethodException(HandlerMethod handlerMethod,
			Exception ex, Message<?> message) {
		super.processHandlerMethodException(handlerMethod, ex, message);
		throw new MessagingException(
				"An exception occurred while invoking the handler method", ex);
	}

	private CompositeMessageConverter createPayloadArgumentCompositeConverter() {
		List<MessageConverter> payloadArgumentConverters = new ArrayList<>(
				this.messageConverters);

		ObjectMessageConverter objectMessageConverter = new ObjectMessageConverter();
		objectMessageConverter.setStrictContentTypeMatch(true);
		payloadArgumentConverters.add(objectMessageConverter);

		payloadArgumentConverters.add(new SimpleMessageConverter());

		return new CompositeMessageConverter(payloadArgumentConverters);
	}

	@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
	protected static class MappingInformation implements Comparable<MappingInformation> {

		private final Set<String> logicalResourceIds;

		private final SqsMessageDeletionPolicy deletionPolicy;

		public MappingInformation(Set<String> logicalResourceIds,
				SqsMessageDeletionPolicy deletionPolicy) {
			this.logicalResourceIds = Collections.unmodifiableSet(logicalResourceIds);
			this.deletionPolicy = deletionPolicy;
		}

		public Set<String> getLogicalResourceIds() {
			return this.logicalResourceIds;
		}

		public SqsMessageDeletionPolicy getDeletionPolicy() {
			return this.deletionPolicy;
		}

		@SuppressWarnings("NullableProblems")
		@Override
		public int compareTo(MappingInformation o) {
			return 0;
		}

		@Override
		public String toString() {
			return logicalResourceIds.stream().collect(Collectors.joining(", "));
		}

	}

	private static final class NoOpValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}

	}

}
