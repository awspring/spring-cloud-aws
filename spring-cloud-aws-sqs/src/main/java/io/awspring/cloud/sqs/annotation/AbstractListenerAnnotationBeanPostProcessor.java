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
import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.HandlerMethodEndpoint;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.support.resolver.AcknowledgmentHandlerMethodArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.BatchAcknowledgmentArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.BatchPayloadMethodArgumentResolver;
import java.lang.annotation.Annotation;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
 * {@link BeanPostProcessor} implementation that scans beans for a {@link SqsListener @SqsListener} annotation, extracts
 * information to a {@link SqsEndpoint}, and registers it in the {@link EndpointRegistrar}.
 *
 * @author Tomaz Fernandes
 * @author Joao Calassio
 * @author José Iêdo
 * @since 3.0
 */
public abstract class AbstractListenerAnnotationBeanPostProcessor<A extends Annotation>
		implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private final AtomicInteger counter = new AtomicInteger();

	private final Collection<Class<?>> nonAnnotatedClasses = Collections.synchronizedSet(new HashSet<>());

	private final EndpointRegistrar endpointRegistrar = createEndpointRegistrar();

	private final DelegatingMessageHandlerMethodFactory delegatingHandlerMethodFactory = new DelegatingMessageHandlerMethodFactory();

	private BeanFactory beanFactory;

	@Nullable
	private BeanExpressionResolver expressionResolver;

	@Nullable
	private BeanExpressionContext expressionContext;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (this.nonAnnotatedClasses.contains(targetClass)) {
			return bean;
		}
		detectAnnotationsAndRegisterEndpoints(bean, targetClass);
		return bean;
	}

	@Nullable
	protected ConfigurableBeanFactory getConfigurableBeanFactory() {
		return this.beanFactory instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) this.beanFactory : null;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected void detectAnnotationsAndRegisterEndpoints(Object bean, Class<?> targetClass) {
		Map<Method, A> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
				(MethodIntrospector.MetadataLookup<A>) method -> AnnotatedElementUtils.findMergedAnnotation(method,
						getAnnotationClass()));

		A classListener = AnnotatedElementUtils.findMergedAnnotation(targetClass, getAnnotationClass());
		boolean hasMethodLevelListeners = !annotatedMethods.isEmpty();
		boolean hasClassLevelListeners = classListener != null;

		if (!hasMethodLevelListeners && !hasClassLevelListeners) {
			this.nonAnnotatedClasses.add(targetClass);
		}
		else {
			if (hasMethodLevelListeners) {
				annotatedMethods.entrySet().stream()
						.map(entry -> createAndConfigureEndpoint(bean, entry.getKey(), entry.getValue()))
						.forEach(this.endpointRegistrar::registerEndpoint);
			}

			if (hasClassLevelListeners) {
				Set<Method> handlerMethods = getHandlerMethods(targetClass);
				createAndConfigureMultiMethodEndpoint(bean, targetClass, classListener,
						new ArrayList<>(handlerMethods));
			}
		}
	}

	protected abstract Class<A> getAnnotationClass();

	protected abstract Set<Method> getHandlerMethods(Class<?> targetClass);

	private Endpoint createAndConfigureEndpoint(Object bean, Method method, A annotation) {
		Endpoint endpoint = createEndpoint(annotation);
		ConfigUtils.INSTANCE.acceptIfInstance(endpoint, HandlerMethodEndpoint.class, hme -> {
			hme.setBean(bean);
			hme.setMethod(method);
			hme.setHandlerMethodFactory(this.delegatingHandlerMethodFactory);
		});
		return endpoint;
	}

	private void createAndConfigureMultiMethodEndpoint(Object bean, Class<?> targetClass, A classListener,
			List<Method> handlerMethods) {
		Assert.notEmpty(handlerMethods, "No handler method found for listener in class: " + targetClass);
		Method defaultMethod = getDefaultHandlerMethod(targetClass, handlerMethods);

		Endpoint endpoint = createMultiMethodEndpoint(classListener, handlerMethods, defaultMethod, bean);
		for (Method method : handlerMethods) {
			ConfigUtils.INSTANCE.acceptIfInstance(endpoint, HandlerMethodEndpoint.class, hme -> {
				hme.setBean(bean);
				hme.setMethod(method);
				hme.setHandlerMethodFactory(this.delegatingHandlerMethodFactory);
			});
		}

		this.endpointRegistrar.registerEndpoint(endpoint);
	}

	protected abstract Method getDefaultHandlerMethod(Class<?> targetClass, List<Method> handlerMethods);

	protected abstract Endpoint createEndpoint(A sqsListenerAnnotation);

	protected abstract Endpoint createMultiMethodEndpoint(A sqsListenerAnnotation, List<Method> methods,
			@Nullable Method defaultMethod, Object bean);

	protected Collection<String> resolveEndpointNames(String[] endpointNames) {
		return Arrays.stream(endpointNames).map(this::resolveExpression)
				.flatMap(resolvedName -> resolveAsStrings(resolvedName).stream()).collect(Collectors.toList());
	}

	@Nullable
	private Object resolveExpression(String value) {
		return getExpressionResolver() != null
				? getExpressionResolver().evaluate(resolve(value), getExpressionContext())
				: value;
	}

	@Nullable
	protected BeanExpressionResolver getExpressionResolver() {
		if (this.expressionResolver == null && this.beanFactory instanceof ConfigurableListableBeanFactory clbf) {
			this.expressionResolver = clbf.getBeanExpressionResolver();
		}
		return this.expressionResolver;
	}

	@Nullable
	private BeanExpressionContext getExpressionContext() {
		if (this.expressionContext == null && this.beanFactory instanceof ConfigurableBeanFactory clbf) {
			this.expressionContext = new BeanExpressionContext(clbf, null);
		}
		return this.expressionContext;
	}

	@Nullable
	private String resolve(String value) {
		if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
			return cbf.resolveEmbeddedValue(value);
		}
		return value;
	}

	protected String resolveAsString(String value, String propertyName) {
		try {
			Collection<String> resolvedStrings = resolveAsStrings(resolve(value));
			return resolvedStrings.isEmpty() ? value : resolvedStrings.iterator().next();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not resolve property " + propertyName, e);
		}
	}

	private Collection<String> resolveAsStrings(@Nullable Object resolvedValue) {
		if (resolvedValue instanceof String[] strArr) {
			return resolveFromStream(Arrays.stream(strArr));
		}
		else if (resolvedValue instanceof Iterable<?> itr) {
			return resolveFromStream(StreamSupport.stream(itr.spliterator(), false));
		}
		else if (resolvedValue instanceof String str) {
			return Collections.singletonList(str);
		}
		else {
			throw new IllegalArgumentException("Cannot resolve " + resolvedValue + " as String");
		}
	}

	private List<String> resolveFromStream(Stream<?> stream) {
		return stream.flatMap(str -> resolveAsStrings(str).stream()).collect(Collectors.toList());
	}

	@Nullable
	protected Integer resolveAsInteger(String value, String propertyName) {
		try {
			Object resolvedValue = resolveExpression(value);
			return resolvedValue instanceof Number numberValue ? Integer.valueOf(numberValue.intValue())
					: resolvedValue instanceof String stringValue && StringUtils.hasText(stringValue)
							? Integer.parseInt(stringValue)
							: null;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Cannot resolve " + propertyName + " as Integer");
		}
	}

	@Nullable
	protected AcknowledgementMode resolveAcknowledgement(String value) {
		try {
			final String resolvedValue = resolveAsString(value, "acknowledgementMode");
			return StringUtils.hasText(resolvedValue) ? AcknowledgementMode.valueOf(resolvedValue) : null;
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Cannot resolve " + value + " as AcknowledgementMode", e);
		}
	}

	protected String getEndpointId(String id) {
		if (StringUtils.hasText(id)) {
			return resolveAsString(id, "id");
		}
		else {
			return getGeneratedIdPrefix() + this.counter.getAndIncrement();
		}
	}

	protected abstract String getGeneratedIdPrefix();

	@Override
	public void afterSingletonsInstantiated() {
		this.endpointRegistrar
				.setMessageListenerContainerRegistryBeanName(getMessageListenerContainerRegistryBeanName());
		if (this.beanFactory instanceof ListableBeanFactory lbf) {
			lbf.getBeansOfType(SqsListenerConfigurer.class).values()
					.forEach(customizer -> customizer.configure(this.endpointRegistrar));
		}
		this.endpointRegistrar.setBeanFactory(getBeanFactory());
		initializeHandlerMethodFactory();
		this.endpointRegistrar.afterSingletonsInstantiated();
	}

	protected abstract String getMessageListenerContainerRegistryBeanName();

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected void initializeHandlerMethodFactory() {
		MessageHandlerMethodFactory handlerMethodFactory = this.endpointRegistrar.getMessageHandlerMethodFactory();
		ConfigUtils.INSTANCE.acceptIfInstance(handlerMethodFactory, DefaultMessageHandlerMethodFactory.class,
				this::configureDefaultHandlerMethodFactory);
		this.delegatingHandlerMethodFactory.setDelegate(handlerMethodFactory);
	}

	protected void configureDefaultHandlerMethodFactory(DefaultMessageHandlerMethodFactory handlerMethodFactory) {
		CompositeMessageConverter compositeMessageConverter = createCompositeMessageConverter();

		List<HandlerMethodArgumentResolver> methodArgumentResolvers = new ArrayList<>(
				createAdditionalArgumentResolvers(compositeMessageConverter, this.endpointRegistrar.getObjectMapper()));
		methodArgumentResolvers.addAll(createArgumentResolvers(compositeMessageConverter));
		this.endpointRegistrar.getMethodArgumentResolversConsumer().accept(methodArgumentResolvers);
		handlerMethodFactory.setArgumentResolvers(methodArgumentResolvers);
		handlerMethodFactory.afterPropertiesSet();
	}

	protected Collection<HandlerMethodArgumentResolver> createAdditionalArgumentResolvers(
			MessageConverter messageConverter, ObjectMapper objectMapper) {
		return createAdditionalArgumentResolvers();
	}

	protected Collection<HandlerMethodArgumentResolver> createAdditionalArgumentResolvers() {
		return Collections.emptyList();
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
				new AcknowledgmentHandlerMethodArgumentResolver(),
				new BatchAcknowledgmentArgumentResolver(),
				new HeaderMethodArgumentResolver(new DefaultConversionService(), getConfigurableBeanFactory()),
				new HeadersMethodArgumentResolver(),
				new BatchPayloadMethodArgumentResolver(messageConverter, this.endpointRegistrar.getValidator()),
				new MessageMethodArgumentResolver(messageConverter),
				new PayloadMethodArgumentResolver(messageConverter,  this.endpointRegistrar.getValidator()));
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

	protected EndpointRegistrar createEndpointRegistrar() {
		return new EndpointRegistrar();
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
