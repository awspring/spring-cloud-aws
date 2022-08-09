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
package io.awspring.cloud.sqs.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.ExpressionResolvingHelper;
import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.HandlerMethodEndpoint;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.config.SqsListenerCustomizer;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.support.resolver.AcknowledgementHandlerMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.AsyncAcknowledgmentHandlerMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.BatchPayloadMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.QueueAttributesMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.SqsMessageMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.VisibilityHandlerMethodArgumentResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link BeanPostProcessor} implementation that scans the bean for a {@link SqsListener @SqsListener} annotation,
 * extracts information to a {@link SqsEndpoint}, and registers it in the {@link EndpointRegistrar}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsListenerAnnotationBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private static final String GENERATED_ID_PREFIX = "io.awspring.cloud.sqs.sqsListenerEndpointContainer#";

	private final AtomicInteger counter = new AtomicInteger();

	private final Collection<Class<?>> nonAnnotatedClasses = Collections.synchronizedSet(new HashSet<>());

	private final ExpressionResolvingHelper expressionResolvingHelper = new ExpressionResolvingHelper();

	private final EndpointRegistrar endpointRegistrar = new EndpointRegistrar();

	private final DelegatingMessageHandlerMethodFactory delegatingHandlerMethodFactory = new DelegatingMessageHandlerMethodFactory();

	private BeanFactory beanFactory;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (this.nonAnnotatedClasses.contains(targetClass)) {
			return bean;
		}
		detectAnnotationsAndRegisterEndpoints(bean, targetClass);
		return bean;
	}

	protected ExpressionResolvingHelper resolveExpression() {
		return this.expressionResolvingHelper;
	}

	@Nullable
	protected ConfigurableBeanFactory getConfigurableBeanFactory() {
		return this.beanFactory instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) this.beanFactory : null;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected void detectAnnotationsAndRegisterEndpoints(Object bean, Class<?> targetClass) {
		Map<Method, SqsListener> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
				(MethodIntrospector.MetadataLookup<SqsListener>) method -> AnnotatedElementUtils
						.findMergedAnnotation(method, SqsListener.class));
		if (annotatedMethods.isEmpty()) {
			this.nonAnnotatedClasses.add(targetClass);
		}
		annotatedMethods.entrySet().stream()
				.map(entry -> createEndpointFromAnnotation(bean, entry.getKey(), entry.getValue()))
				.forEach(this.endpointRegistrar::registerEndpoint);
	}

	private Endpoint createEndpointFromAnnotation(Object bean, Method method, SqsListener annotation) {
		Endpoint endpoint = doCreateEndpointFromAnnotation(annotation);
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, HandlerMethodEndpoint.class, hme -> {
			hme.setBean(bean);
			hme.setMethod(method);
			hme.setHandlerMethodFactory(this.delegatingHandlerMethodFactory);
		});
		return endpoint;
	}

	private Endpoint doCreateEndpointFromAnnotation(SqsListener sqsListenerAnnotation) {
		return SqsEndpoint.from(resolveDestinationNames(sqsListenerAnnotation.value()))
				.factoryBeanName(resolveExpression().asString(sqsListenerAnnotation.factory(), "factory"))
				.id(getEndpointId(sqsListenerAnnotation.id()))
				.pollTimeoutSeconds(
						resolveExpression().asInteger(sqsListenerAnnotation.pollTimeoutSeconds(), "pollTimeoutSeconds"))
				.maxInflightMessagesPerQueue(resolveExpression()
						.asInteger(sqsListenerAnnotation.maxInflightMessagesPerQueue(), "maxInflightMessagesPerQueue"))
				.messageVisibility(resolveExpression().asInteger(sqsListenerAnnotation.messageVisibilitySeconds(),
						"messageVisibility"))
				.build();
	}

	private Set<String> resolveDestinationNames(String[] destinationNames) {
		return Arrays.stream(destinationNames)
				.map(destinationName -> resolveExpression().asString(destinationName, "queueNames"))
				.collect(Collectors.toSet());
	}

	protected String getEndpointId(String id) {
		if (StringUtils.hasText(id)) {
			return resolveExpression().asString(id, "id");
		}
		else {
			return GENERATED_ID_PREFIX + this.counter.getAndIncrement();
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		ConfigUtils.INSTANCE.acceptIfInstance(getBeanFactory(), ListableBeanFactory.class,
				lbf -> lbf.getBeansOfType(SqsListenerCustomizer.class).values()
						.forEach(customizer -> customizer.configure(this.endpointRegistrar)));
		this.endpointRegistrar.setBeanFactory(getBeanFactory());
		initializeHandlerMethodFactory();
		this.endpointRegistrar.afterSingletonsInstantiated();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		this.expressionResolvingHelper.setBeanFactory(beanFactory);
	}

	protected void initializeHandlerMethodFactory() {
		MessageHandlerMethodFactory handlerMethodFactory = this.endpointRegistrar.getMessageHandlerMethodFactory();
		ConfigUtils.INSTANCE.acceptIfInstance(handlerMethodFactory, DefaultMessageHandlerMethodFactory.class,
				this::configureDefaultHandlerMethodFactory);
		this.delegatingHandlerMethodFactory.setDelegate(handlerMethodFactory);
	}

	protected void configureDefaultHandlerMethodFactory(DefaultMessageHandlerMethodFactory handlerMethodFactory) {
		CompositeMessageConverter compositeMessageConverter = createCompositeMessageConverter();
		List<HandlerMethodArgumentResolver> methodArgumentResolvers = createArgumentResolvers(
				compositeMessageConverter);
		this.endpointRegistrar.getMethodArgumentResolversConsumer().accept(methodArgumentResolvers);
		handlerMethodFactory.setArgumentResolvers(methodArgumentResolvers);
		handlerMethodFactory.afterPropertiesSet();
	}

	protected CompositeMessageConverter createCompositeMessageConverter() {
		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(new StringMessageConverter());
		messageConverters.add(new SimpleMessageConverter());
		messageConverters.add(createDefaultMappingJackson2MessageConverter(this.endpointRegistrar.getObjectMapper()));
		this.endpointRegistrar.getMessageConverterConsumer().accept(messageConverters);
		return new CompositeMessageConverter(messageConverters);
	}

	// @formatter:off
	protected List<HandlerMethodArgumentResolver> createArgumentResolvers(MessageConverter messageConverter) {
		return Arrays.asList(
				new AcknowledgementHandlerMethodArgumentResolver(),
				new AsyncAcknowledgmentHandlerMethodArgumentResolver(),
				new VisibilityHandlerMethodArgumentResolver(SqsHeaders.SQS_VISIBILITY_HEADER),
				new SqsMessageMethodArgumentResolver(),
				new QueueAttributesMethodArgumentResolver(),
				new HeaderMethodArgumentResolver(new DefaultConversionService(), getConfigurableBeanFactory()),
				new HeadersMethodArgumentResolver(),
				new BatchPayloadMethodArgumentResolver(messageConverter),
				new MessageMethodArgumentResolver(messageConverter),
				new PayloadMethodArgumentResolver(messageConverter));
	}
	// @formatter:on

	protected MappingJackson2MessageConverter createDefaultMappingJackson2MessageConverter(ObjectMapper objectMapper) {
		MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(false);
		if (objectMapper != null) {
			jacksonMessageConverter.setObjectMapper(objectMapper);
		}
		return jacksonMessageConverter;
	}

	private static class DelegatingMessageHandlerMethodFactory implements MessageHandlerMethodFactory {

		private MessageHandlerMethodFactory delegate;

		@Override
		public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
			Assert.notNull(this.delegate, "No delegate MessageHandlerMethodFactory set.");
			return this.delegate.createInvocableHandlerMethod(bean, method);
		}

		public void setDelegate(MessageHandlerMethodFactory delegate) {
			this.delegate = delegate;
		}
	}

}
