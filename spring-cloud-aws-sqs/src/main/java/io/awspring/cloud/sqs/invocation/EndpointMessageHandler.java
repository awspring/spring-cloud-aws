/*
 * Copyright 2013-2020 the original author or authors.
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
package io.awspring.cloud.sqs.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.support.endpoint.Endpoint;
import io.awspring.cloud.messaging.support.endpoint.EndpointRegistry;
import io.awspring.cloud.messaging.support.listener.MessageHeaders;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.endpoint.SqsEndpoint;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.support.AsyncAcknowledgmentHandlerMethodArgumentResolver;
import io.awspring.cloud.sqs.support.SqsHeadersMethodArgumentResolver;
import io.awspring.cloud.sqs.support.SqsMessageMethodArgumentResolver;
import io.awspring.cloud.sqs.support.VisibilityHandlerMethodArgumentResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Wojciech Mąka
 * @author Matej Nedic
 * @author Tomaz Fernandes
 * @since 1.0
 */
public class EndpointMessageHandler extends AbstractMethodMessageHandler<Endpoint>
		implements EndpointRegistry, DisposableBean {

	private final List<MessageConverter> messageConverters;

	private ObjectMapper objectMapper;

	private final SqsAsyncClient sqsAsyncClient;

	public EndpointMessageHandler(SqsAsyncClient sqsAsyncClient, List<MessageConverter> messageConverters) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.messageConverters = messageConverters;
	}

	public EndpointMessageHandler(SqsAsyncClient sqsAsyncClient) {
		this(sqsAsyncClient, Collections.emptyList());
	}

	private static String[] wrapInStringArray(Object valueToWrap) {
		return new String[] { valueToWrap.toString() };
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(getCustomArgumentResolvers());

		resolvers.add(new SqsHeadersMethodArgumentResolver());
		resolvers.add(new AsyncAcknowledgmentHandlerMethodArgumentResolver(MessageHeaders.ACKNOWLEDGMENT_HEADER));
		resolvers.add(new VisibilityHandlerMethodArgumentResolver(SqsMessageHeaders.VISIBILITY));
		resolvers.add(new SqsMessageMethodArgumentResolver());
		resolvers.add(new HeaderMethodArgumentResolver(new GenericConversionService(), null));
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverters.isEmpty() ? new StringMessageConverter()
				: new CompositeMessageConverter(this.messageConverters)));
		CompositeMessageConverter compositeMessageConverter = createPayloadArgumentCompositeConverter();
		resolvers.add(new PayloadMethodArgumentResolver(compositeMessageConverter));
		return resolvers;
	}

	private CompositeMessageConverter createPayloadArgumentCompositeConverter() {
		List<MessageConverter> payloadArgumentConverters = new ArrayList<>(this.messageConverters);
		payloadArgumentConverters.add(getDefaultMappingJackson2MessageConverter());
		payloadArgumentConverters.add(new SimpleMessageConverter());
		return new CompositeMessageConverter(payloadArgumentConverters);
	}

	private MappingJackson2MessageConverter getDefaultMappingJackson2MessageConverter() {
		MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(false);

		if (this.objectMapper != null) {
			jacksonMessageConverter.setObjectMapper(objectMapper);
		}
		return jacksonMessageConverter;
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
	protected SqsEndpoint getMappingForMethod(Method method, Class<?> handlerType) {
		SqsListener sqsListenerAnnotation = AnnotationUtils.findAnnotation(method, SqsListener.class);
		if (sqsListenerAnnotation != null && sqsListenerAnnotation.value().length > 0) {
			Set<String> logicalEndpointNames = resolveDestinationNames(sqsListenerAnnotation.value());
			return SqsEndpoint.from(logicalEndpointNames)
					.factoryBeanName(resolveName(sqsListenerAnnotation.factory())[0])
					.pollTimeoutSeconds(resolveInteger(sqsListenerAnnotation.pollTimeoutSeconds()))
					.simultaneousPollsPerQueue(resolveInteger(sqsListenerAnnotation.concurrentPollsPerContainer()))
					.minTimeToProcess(resolveInteger(sqsListenerAnnotation.minSecondsToProcess()))
					.async(CompletionStage.class.isAssignableFrom(method.getReturnType()))
					.queuesAttributes(logicalEndpointNames.stream()
							.collect(Collectors.toMap(name -> name, this::getQueueAttributes)))
					.build();
		}

		return null;
	}

	private Integer resolveInteger(String pollingTimeoutSeconds) {
		String[] pollingStrings = resolveName(pollingTimeoutSeconds);
		return pollingStrings == null || pollingStrings.length == 0 || !StringUtils.hasText(pollingStrings[0]) ? null
				: Integer.parseInt(pollingStrings[0]);
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
		ConfigurableBeanFactory configurableBeanFactory = applicationContext.getBeanFactory();

		String placeholdersResolved = configurableBeanFactory.resolveEmbeddedValue(name);
		BeanExpressionResolver exprResolver = configurableBeanFactory.getBeanExpressionResolver();
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

	private QueueAttributes getQueueAttributes(String queue) {
		try {
			logger.debug("Fetching queue attributes for queue " + queue);
			String queueUrl = this.sqsAsyncClient.getQueueUrl(req -> req.queueName(queue)).get().queueUrl();
			Map<QueueAttributeName, String> attributes = this.sqsAsyncClient
						.getQueueAttributes(req -> req.queueUrl(queueUrl)).get().attributes();
			boolean hasRedrivePolicy = attributes.containsKey(QueueAttributeName.REDRIVE_POLICY);
			boolean isFifo = queue.endsWith(".fifo");
			return new QueueAttributes(queueUrl, hasRedrivePolicy, getVisibility(attributes), isFifo);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while fetching attributes for queue " + queue);
		}
		catch (ExecutionException e) {
			throw new IllegalStateException("ExecutionException while fetching attributes for queue " + queue);
		}
	}

	private Integer getVisibility(Map<QueueAttributeName, String> attributes) {
		String visibilityTimeout = attributes.get(QueueAttributeName.VISIBILITY_TIMEOUT);
		return visibilityTimeout != null ? Integer.parseInt(visibilityTimeout) : null;
	}

	@Override
	protected Set<String> getDirectLookupDestinations(Endpoint mapping) {
		return new HashSet<>(mapping.getLogicalEndpointNames());
	}

	@Override
	protected String getDestination(Message<?> message) {
		return message.getHeaders().get(SqsMessageHeaders.SQS_LOGICAL_RESOURCE_ID).toString();
	}

	@Override
	protected Endpoint getMatchingMapping(Endpoint endpoint, Message<?> message) {
		return endpoint.getLogicalEndpointNames().contains(getDestination(message)) ? endpoint : null;
	}

	@Override
	protected Comparator<Endpoint> getMappingComparator(Message<?> message) {
		return (o1, o2) -> 0;
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

	@Override
	protected void handleNoMatch(Set<Endpoint> ts, String lookupDestination, Message<?> message) {
		this.logger.warn("No match found");
	}

	@Override
	protected void processHandlerMethodException(HandlerMethod handlerMethod, Exception ex, Message<?> message) {
		InvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, ex);
		if (exceptionHandlerMethod != null) {
			super.processHandlerMethodException(handlerMethod, ex, message);
		}
		throw new MessagingException(message, "An exception occurred while invoking the handler method", ex);
	}

	@Override
	public Collection<Endpoint> retrieveEndpoints() {
		return Collections.unmodifiableSet(super.getHandlerMethods().keySet());
	}

	@Override
	public void destroy() throws Exception {
		logger.error("Destroying");
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
