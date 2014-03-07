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

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueMessageHandler extends AbstractMethodMessageHandler<QueueMessageHandler.MappingInformation> {

	public static final String LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY = "LogicalResourceId";

	private final MessageConverter messageConverter;

	public QueueMessageHandler() {
		this(new StringMessageConverter());
	}

	public QueueMessageHandler(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		return Collections.singletonList(new PayloadArgumentResolver(this.messageConverter, new NoopValidator()));
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		return Collections.singletonList(new SendToHandlerMethodReturnValueHandler());
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return true;
	}

	@Override
	protected MappingInformation getMappingForMethod(Method method, Class<?> handlerType) {
		MessageMapping annotation = AnnotationUtils.findAnnotation(method, MessageMapping.class);
		if (annotation == null) {
			return null;
		}

		if (annotation.value().length != 1) {
			throw new IllegalStateException("@MessageMapping annotation must have exactly one destination");
		}

		String logicalResourceId = annotation.value()[0];
		return new MappingInformation(logicalResourceId, null);
	}

	@Override
	protected Set<String> getDirectLookupDestinations(MappingInformation mapping) {
		return Collections.singleton(mapping.getLogicalResourceId());
	}

	@Override
	protected String getDestination(Message<?> message) {
		return message.getHeaders().get(LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY).toString();
	}

	@Override
	protected MappingInformation getMatchingMapping(MappingInformation mapping, Message<?> message) {
		if (getDestination(message).equals(mapping.getLogicalResourceId())) {
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
	protected static class MappingInformation implements Comparable<MappingInformation> {

		private final String logicalResourceId;
		private final String physicalResourceId;

		private MappingInformation(String logicalResourceId, String physicalResourceId) {
			this.logicalResourceId = logicalResourceId;
			this.physicalResourceId = physicalResourceId;
		}

		public String getLogicalResourceId() {
			return this.logicalResourceId;
		}

		public String getPhysicalResourceId() {
			return this.physicalResourceId;
		}

		@Override
		public int compareTo(MappingInformation o) {
			return this.logicalResourceId.compareTo(o.getLogicalResourceId());
		}
	}

	private class SendToHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return returnType.getMethodAnnotation(SendTo.class) != null;
		}

		@Override
		public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
			QueueMessageHandler.this.handleMessage(MessageBuilder.withPayload(returnValue).setHeader(LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, getValue(returnType)).build());
		}

		private String getValue(MethodParameter returnType) {
			return returnType.getMethodAnnotation(SendTo.class).value()[0];
		}
	}

	private static final class NoopValidator implements Validator {

		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}
	}
}
