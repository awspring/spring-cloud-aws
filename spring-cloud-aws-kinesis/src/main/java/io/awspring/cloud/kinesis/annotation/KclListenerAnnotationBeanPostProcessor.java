/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.annotation;

import io.awspring.cloud.kinesis.config.KclEndpointRegistrar;
import io.awspring.cloud.kinesis.config.KclListenerConfigurer;
import io.awspring.cloud.kinesis.config.KclListenerEndpoint;
import io.awspring.cloud.kinesis.config.MessageListenerContainerFactory;
import io.awspring.cloud.kinesis.listener.MessageListenerContainer;
import io.awspring.cloud.kinesis.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.kinesis.listener.checkpoint.KclCheckpointMode;
import io.awspring.cloud.kinesis.listener.retrieval.RetrievalMode;
import io.awspring.cloud.kinesis.support.resolver.BatchMessagesArgumentResolver;
import io.awspring.cloud.kinesis.support.resolver.CheckpointerArgumentResolver;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.Validator;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class KclListenerAnnotationBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(KclListenerAnnotationBeanPostProcessor.class);

	private static final String DEFAULT_ID_PREFIX = "io.awspring.cloud.kinesis.KclListenerEndpointContainer#";

	private final AtomicInteger counter = new AtomicInteger();

	private final Set<Class<?>> nonAnnotatedClasses = Collections.synchronizedSet(new HashSet<>());

	private final List<KclListenerEndpoint> endpoints = new ArrayList<>();

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private BeanExpressionResolver expressionResolver;

	@Nullable
	private BeanExpressionContext expressionContext;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (this.nonAnnotatedClasses.contains(targetClass)) {
			return bean;
		}
		Map<Method, KclListener> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
				(MethodIntrospector.MetadataLookup<KclListener>) method -> AnnotatedElementUtils
						.findMergedAnnotation(method, KclListener.class));
		if (annotatedMethods.isEmpty()) {
			this.nonAnnotatedClasses.add(targetClass);
			return bean;
		}
		annotatedMethods.forEach((method, annotation) -> this.endpoints.add(createEndpoint(bean, method, annotation)));
		return bean;
	}

	@Override
	public void afterSingletonsInstantiated() {
		KclEndpointRegistrar registrar = new KclEndpointRegistrar();
		if (this.beanFactory instanceof ListableBeanFactory listableBeanFactory) {
			listableBeanFactory.getBeansOfType(KclListenerConfigurer.class).values()
					.forEach(configurer -> configurer.configure(registrar));
		}
		MessageHandlerMethodFactory handlerMethodFactory = resolveHandlerMethodFactory(registrar);
		MessageListenerContainerRegistry registry = getBeanFactory().getBean(MessageListenerContainerRegistry.class);
		for (KclListenerEndpoint endpoint : this.endpoints) {
			endpoint.setHandlerMethodFactory(handlerMethodFactory);
			MessageListenerContainer container = resolveContainerFactory(endpoint).createContainer(endpoint);
			registry.registerListenerContainer(container);
			logger.info("Registered @KclListener container '{}' for stream '{}'", container.getId(),
					endpoint.getStreamNames());
		}
	}

	private KclListenerEndpoint createEndpoint(Object bean, Method method, KclListener annotation) {
		String id = getEndpointId(annotation.id());
		List<String> streamNames = resolveStreamNames(annotation.streamNames());
		String applicationName = StringUtils.hasText(annotation.applicationName())
				? resolveRequired(annotation.applicationName(), "applicationName")
				: id;
		String factory = resolve(annotation.factory());
		String factoryBeanName = StringUtils.hasText(factory) ? factory : null;
		return KclListenerEndpoint.builder().id(id).streamNames(streamNames).applicationName(applicationName)
				.factoryBeanName(factoryBeanName).bean(bean).method(method)
				.checkpointMode(resolveCheckpointMode(annotation.checkpointMode()))
				.retrievalMode(resolveRetrievalMode(annotation.retrievalMode()))
				.initialPositionInStream(resolveInitialPositionInStream(annotation.initialPositionInStream()))
				.consumerName(resolveOptional(annotation.consumerName()))
				.leaseTableName(resolveOptional(annotation.leaseTableName()))
				.metricsNamespace(resolveOptional(annotation.metricsNamespace()))
				.replyStream(resolveReplyStream(method)).build();
	}

	private List<String> resolveStreamNames(String[] streamNames) {
		Assert.notEmpty(streamNames, "At least one stream name must be provided on @KclListener");
		List<String> resolved = new ArrayList<>(streamNames.length);
		for (String streamName : streamNames) {
			resolved.add(resolveRequired(streamName, "streamNames"));
		}
		return resolved;
	}

	@Nullable
	private KclCheckpointMode resolveCheckpointMode(String value) {
		return resolveEnum(value, KclCheckpointMode::valueOf);
	}

	@Nullable
	private RetrievalMode resolveRetrievalMode(String value) {
		return resolveEnum(value, RetrievalMode::valueOf);
	}

	@Nullable
	private InitialPositionInStream resolveInitialPositionInStream(String value) {
		InitialPositionInStream initialPosition = resolveEnum(value, InitialPositionInStream::valueOf);
		Assert.isTrue(initialPosition != InitialPositionInStream.AT_TIMESTAMP,
				"AT_TIMESTAMP is not supported on @KclListener because the annotation cannot carry a timestamp: set 'initialPositionInStream' to an empty string to fall back to the container factory, and configure AT_TIMESTAMP together with a timestamp there or through the 'spring.cloud.aws.kinesis.listener' properties");
		return initialPosition;
	}

	@Nullable
	private <T> T resolveEnum(String value, Function<String, T> parser) {
		String resolved = resolve(value);
		return StringUtils.hasText(resolved) ? parser.apply(resolved) : null;
	}

	/**
	 * Resolves an optional attribute, returning {@code null} when it is left empty so that the value configured on the
	 * container factory is kept.
	 */
	@Nullable
	private String resolveOptional(String value) {
		String resolved = resolve(value);
		return StringUtils.hasText(resolved) ? resolved : null;
	}

	@Nullable
	private String resolveReplyStream(Method method) {
		SendTo sendTo = AnnotatedElementUtils.findMergedAnnotation(method, SendTo.class);
		if (sendTo == null || sendTo.value().length == 0) {
			return null;
		}
		Assert.isTrue(sendTo.value().length == 1, "@SendTo on @KclListener must declare a single destination stream");
		return resolve(sendTo.value()[0]);
	}

	private MessageListenerContainerFactory resolveContainerFactory(KclListenerEndpoint endpoint) {
		String factoryBeanName = endpoint.getFactoryBeanName();
		if (factoryBeanName != null) {
			return getBeanFactory().getBean(factoryBeanName, MessageListenerContainerFactory.class);
		}
		return getBeanFactory().getBean(MessageListenerContainerFactory.class);
	}

	private MessageHandlerMethodFactory resolveHandlerMethodFactory(KclEndpointRegistrar registrar) {
		MessageHandlerMethodFactory customFactory = registrar.getMessageHandlerMethodFactory();
		if (customFactory != null) {
			return customFactory;
		}
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		MessageConverter messageConverter = createMessageConverter(registrar);
		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>(
				List.of(new CheckpointerArgumentResolver(), new BatchMessagesArgumentResolver(messageConverter)));
		registrar.getMethodArgumentResolversConsumer().accept(argumentResolvers);
		factory.setCustomArgumentResolvers(argumentResolvers);
		factory.setMessageConverter(messageConverter);
		Validator validator = registrar.getValidator();
		if (validator != null) {
			factory.setValidator(validator);
		}
		factory.afterPropertiesSet();
		return factory;
	}

	private MessageConverter createMessageConverter(KclEndpointRegistrar registrar) {
		List<MessageConverter> converters = new ArrayList<>();
		converters.add(new ByteArrayMessageConverter());
		converters.add(new StringMessageConverter());
		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter();
		jacksonConverter.setSerializedPayloadClass(String.class);
		jacksonConverter.setStrictContentTypeMatch(false);
		converters.add(jacksonConverter);
		converters.add(new SimpleMessageConverter());
		registrar.getMessageConvertersConsumer().accept(converters);
		return new CompositeMessageConverter(converters);
	}

	private String getEndpointId(String id) {
		if (StringUtils.hasText(id)) {
			return resolveRequired(id, "id");
		}
		return DEFAULT_ID_PREFIX + this.counter.getAndIncrement();
	}

	private String resolveRequired(String value, String propertyName) {
		String resolved = resolve(value);
		Assert.isTrue(StringUtils.hasText(resolved),
				() -> "Property '" + propertyName + "' resolved to empty for value '" + value + "'");
		return resolved;
	}

	@Nullable
	private String resolve(String value) {
		Object resolved = resolveExpression(value);
		return resolved != null ? resolved.toString() : null;
	}

	@Nullable
	private Object resolveExpression(String value) {
		String resolvedValue = resolvePlaceholders(value);
		BeanExpressionResolver resolver = getExpressionResolver();
		if (resolver == null) {
			return resolvedValue;
		}
		return resolver.evaluate(resolvedValue, getExpressionContext());
	}

	@Nullable
	private String resolvePlaceholders(String value) {
		if (this.beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory) {
			return configurableBeanFactory.resolveEmbeddedValue(value);
		}
		return value;
	}

	@Nullable
	private BeanExpressionResolver getExpressionResolver() {
		if (this.expressionResolver == null
				&& this.beanFactory instanceof ConfigurableListableBeanFactory beanFactory) {
			this.expressionResolver = beanFactory.getBeanExpressionResolver();
		}
		return this.expressionResolver;
	}

	@Nullable
	private BeanExpressionContext getExpressionContext() {
		if (this.expressionContext == null && this.beanFactory instanceof ConfigurableBeanFactory beanFactory) {
			this.expressionContext = new BeanExpressionContext(beanFactory, null);
		}
		return this.expressionContext;
	}

	private BeanFactory getBeanFactory() {
		Assert.notNull(this.beanFactory, "beanFactory must not be null");
		return this.beanFactory;
	}

}
