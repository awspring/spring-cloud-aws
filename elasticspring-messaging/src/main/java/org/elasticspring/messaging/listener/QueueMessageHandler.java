/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.support.NotificationMessageArgumentResolver;
import org.elasticspring.messaging.support.NotificationSubjectArgumentResolver;
import org.elasticspring.messaging.support.converter.JsonMessageConverter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageHandler extends AbstractMethodMessageHandler<QueueMessageHandler.MappingInformation> {

	private MessageSendingOperations<String> sendToMessageTemplate;

	public MessageSendingOperations<String> getSendToMessageTemplate() {
		return this.sendToMessageTemplate;
	}

	/**
	 * This sendToMessageTemplate will be used to send the return value. Note that {@link
	 * org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object)} will be used
	 * and therefore a converter must be set on the {@literal sendToMessageTemplate}.
	 *
	 * @param sendToMessageTemplate to use for sending the return value.
	 * @see org.elasticspring.messaging.listener.SendToHandlerMethodReturnValueHandler
	 * @see org.springframework.messaging.handler.annotation.SendTo
	 */
	@RuntimeUse
	public void setSendToMessageTemplate(MessageSendingOperations<String> sendToMessageTemplate) {
		this.sendToMessageTemplate = sendToMessageTemplate;
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();
		resolvers.addAll(getCustomArgumentResolvers());

		resolvers.add(new HeaderMethodArgumentResolver(null, null));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new NotificationMessageArgumentResolver());
		resolvers.add(new NotificationSubjectArgumentResolver());
		resolvers.add(new PayloadArgumentResolver(new JsonMessageConverter(), new NoOpValidator()));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		ArrayList<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();
		handlers.addAll(this.getCustomReturnValueHandlers());

		handlers.add(new SendToHandlerMethodReturnValueHandler(this.sendToMessageTemplate));

		return handlers;
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return true;
	}

	@Override
	protected MappingInformation getMappingForMethod(Method method, Class<?> handlerType) {
		MessageMapping messageMappingAnnotation = AnnotationUtils.findAnnotation(method, MessageMapping.class);
		if (messageMappingAnnotation == null) {
			return null;
		}

		if (messageMappingAnnotation.value().length < 1) {
			throw new IllegalStateException("@MessageMapping annotation must have at least one destination");
		}

		Set<String> logicalResourceIds = new HashSet<String>(messageMappingAnnotation.value().length);
		logicalResourceIds.addAll(Arrays.asList(messageMappingAnnotation.value()));

		return new MappingInformation(logicalResourceIds);
	}

	@Override
	protected Set<String> getDirectLookupDestinations(MappingInformation mapping) {
		return mapping.getLogicalResourceIds();
	}

	@Override
	protected String getDestination(Message<?> message) {
		return message.getHeaders().get(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY).toString();
	}

	@Override
	protected MappingInformation getMatchingMapping(MappingInformation mapping, Message<?> message) {
		if (mapping.getLogicalResourceIds().contains(getDestination(message))) {
			return mapping;
		} else {
			return null;
		}
	}

	@Override
	protected Comparator<MappingInformation> getMappingComparator(Message<?> message) {
		return new ComparableComparator<MappingInformation>();
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

	@Override
	protected void handleNoMatch(Set<MappingInformation> ts, String lookupDestination, Message<?> message) {
		this.logger.warn("No match found");
	}

	@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
	static class MappingInformation implements Comparable<MappingInformation> {

		private final Set<String> logicalResourceIds;

		private MappingInformation(Set<String> logicalResourceIds) {
			this.logicalResourceIds = Collections.unmodifiableSet(logicalResourceIds);
		}

		public Set<String> getLogicalResourceIds() {
			return this.logicalResourceIds;
		}

		@SuppressWarnings("NullableProblems")
		@Override
		public int compareTo(MappingInformation o) {
			return 0;
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
