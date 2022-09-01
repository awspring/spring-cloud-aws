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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.config.Endpoint;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.MessageListenerContainerFactory;
import io.awspring.cloud.sqs.config.SqsListenerCustomizer;
import io.awspring.cloud.sqs.listener.DefaultListenerContainerRegistry;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class SqsListenerAnnotationBeanPostProcessorTests {

	@Test
	void shouldCustomizeRegistrar() {
		ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
		ObjectMapper objectMapper = new ObjectMapper();
		MessageHandlerMethodFactory methodFactory = new DefaultMessageHandlerMethodFactory();
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry() {
			@Override
			public void registerListenerContainer(MessageListenerContainer<?> listenerContainer) {
			}
		};
		String factoryName = "otherFactory";
		MessageConverter converter = mock(MessageConverter.class);
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);

		SqsListenerCustomizer customizer = registrar -> {
			registrar.setDefaultListenerContainerFactoryBeanName(factoryName);
			registrar.setListenerContainerRegistry(registry);
			registrar.setMessageHandlerMethodFactory(methodFactory);
			registrar.setObjectMapper(objectMapper);
			registrar.manageMessageConverters(converters -> converters.add(converter));
			registrar.manageMethodArgumentResolvers(resolvers -> resolvers.add(resolver));
		};

		when(beanFactory.getBeansOfType(SqsListenerCustomizer.class))
				.thenReturn(Collections.singletonMap("customizer", customizer));
		when(beanFactory.getBean(factoryName, MessageListenerContainerFactory.class))
				.thenReturn(mock(MessageListenerContainerFactory.class));
		when(beanFactory.containsBean(factoryName)).thenReturn(true);

		List<Endpoint> endpoints = new ArrayList<>();

		EndpointRegistrar registrar = new EndpointRegistrar() {
			@Override
			public void registerEndpoint(Endpoint endpoint) {
				endpoints.add(endpoint);
				super.registerEndpoint(endpoint);
			}
		};
		SqsListenerAnnotationBeanPostProcessor processor = new SqsListenerAnnotationBeanPostProcessor() {
			@Override
			protected EndpointRegistrar createEndpointRegistrar() {
				return registrar;
			}

		};

		processor.setBeanFactory(beanFactory);
		Listener bean = new Listener();
		processor.postProcessAfterInitialization(bean, "listener");
		processor.afterSingletonsInstantiated();

		Endpoint endpoint = endpoints.get(0);
		assertThat(endpoint).extracting("handlerMethodFactory").extracting("delegate").isEqualTo(methodFactory)
				.extracting("argumentResolvers").extracting("argumentResolvers")
				.asInstanceOf(list(HandlerMethodArgumentResolver.class)).hasSizeGreaterThan(1).contains(resolver)
				.filteredOn(thisResolver -> thisResolver instanceof PayloadMethodArgumentResolver).element(0)
				.extracting("converter").asInstanceOf(type(CompositeMessageConverter.class))
				.extracting(CompositeMessageConverter::getConverters).asInstanceOf(list(MessageConverter.class))
				.contains(converter)
				.filteredOn(thisConverter -> thisConverter instanceof MappingJackson2MessageConverter).element(0)
				.extracting("objectMapper").isEqualTo(objectMapper);

	}

	@Test
	void shouldChangeListenerRegistryBeanName() {
		ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
		MessageListenerContainerRegistry registry = mock(MessageListenerContainerRegistry.class);
		MessageListenerContainerFactory<?> factory = mock(MessageListenerContainerFactory.class);

		String registryBeanName = "customRegistry";
		SqsListenerCustomizer customizer = registrar -> registrar
				.setMessageListenerContainerRegistryBeanName(registryBeanName);

		when(beanFactory.getBeansOfType(SqsListenerCustomizer.class))
				.thenReturn(Collections.singletonMap("customizer", customizer));
		when(beanFactory.getBean(registryBeanName, MessageListenerContainerRegistry.class)).thenReturn(registry);
		when(beanFactory.containsBean(EndpointRegistrar.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)).thenReturn(true);
		when(beanFactory.getBean(EndpointRegistrar.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME,
				MessageListenerContainerFactory.class)).thenReturn(factory);

		EndpointRegistrar registrar = new EndpointRegistrar();

		SqsListenerAnnotationBeanPostProcessor processor = new SqsListenerAnnotationBeanPostProcessor() {
			@Override
			protected EndpointRegistrar createEndpointRegistrar() {
				return registrar;
			}
		};

		Listener bean = new Listener();
		processor.setBeanFactory(beanFactory);
		processor.postProcessAfterInitialization(bean, "listener");
		processor.afterSingletonsInstantiated();

		assertThat(registrar).extracting("listenerContainerRegistry").isEqualTo(registry);

	}

	@Test
	void shouldThrowIfFactoryBeanNotFound() {
		ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
		MessageListenerContainerRegistry registry = mock(MessageListenerContainerRegistry.class);

		String registryBeanName = "customRegistry";
		SqsListenerCustomizer customizer = registrar -> registrar
				.setMessageListenerContainerRegistryBeanName(registryBeanName);

		when(beanFactory.getBeansOfType(SqsListenerCustomizer.class))
				.thenReturn(Collections.singletonMap("customizer", customizer));
		when(beanFactory.getBean(registryBeanName, MessageListenerContainerRegistry.class)).thenReturn(registry);
		when(beanFactory.containsBean(EndpointRegistrar.DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME))
				.thenReturn(false);

		SqsListenerAnnotationBeanPostProcessor processor = new SqsListenerAnnotationBeanPostProcessor();

		Listener bean = new Listener();
		processor.setBeanFactory(beanFactory);
		processor.postProcessAfterInitialization(bean, "listener");
		assertThatThrownBy(processor::afterSingletonsInstantiated).isInstanceOf(IllegalArgumentException.class);

	}

	static class Listener {

		@SqsListener("myQueue")
		void listen(String message) {
		}

	}

}
