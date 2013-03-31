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

package org.elasticspring.messaging.config.annotation;

import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.elasticspring.messaging.support.converter.ObjectMessageConverter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.support.StaticApplicationContext;

import java.util.HashMap;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanPostProcessorTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testListenerWithNoDestination() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("must contain either value or queueName attribute");
		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		staticApplicationContext.registerSingleton("listener", InvalidBeanWithNoDestination.class);
		staticApplicationContext.registerSingleton("processor", QueueListenerBeanPostProcessor.class);
		staticApplicationContext.refresh();
	}

	@Test
	public void testListenerWithAmbiguousDestination() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("not both");
		StaticApplicationContext staticApplicationContext = new StaticApplicationContext();
		staticApplicationContext.registerSingleton("listener", InvalidBeanWithAmbiguousDestinations.class);
		staticApplicationContext.registerSingleton("processor", QueueListenerBeanPostProcessor.class);
		staticApplicationContext.refresh();
	}

	@Test
	public void testRegisterContainerWithMinimalConfiguration() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		registry.registerBeanDefinition("queueListenerBean",
				BeanDefinitionBuilder.rootBeanDefinition(MinimalListenerConfiguration.class).getBeanDefinition());
		QueueListenerBeanPostProcessor processor = new QueueListenerBeanPostProcessor();
		processor.postProcessBeanDefinitionRegistry(registry);

		Assert.assertEquals(2, registry.getBeanDefinitionCount());
		BeanDefinition definition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(definition);

		Assert.assertEquals(SimpleMessageListenerContainer.class.getName(), definition.getBeanClassName());
		Assert.assertEquals(2, definition.getPropertyValues().size());
		Assert.assertEquals("myQueue", definition.getPropertyValues().getPropertyValue("destinationName").getValue());

		BeanDefinition messageListener = (BeanDefinition) definition.getPropertyValues().getPropertyValue("messageListener").getValue();
		Assert.assertEquals("listenerMethod", messageListener.getConstructorArgumentValues().getArgumentValue(2, String.class).getValue());

		RuntimeBeanReference messageListenerTarget = (RuntimeBeanReference) messageListener.getConstructorArgumentValues().
				getArgumentValue(1, MinimalListenerConfiguration.class).getValue();
		Assert.assertEquals("queueListenerBean", messageListenerTarget.getBeanName());
	}

	@Test
	public void testWithCustomConfiguration() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		registry.registerBeanDefinition("queueListenerBean",
				BeanDefinitionBuilder.rootBeanDefinition(MinimalListenerConfiguration.class).getBeanDefinition());

		HashMap<String, Object> customProperties = new HashMap<String, Object>();
		customProperties.put("amazonSqs", new RuntimeBeanReference("customOne"));
		customProperties.put("maxNumberOfMessages", 2);

		QueueListenerBeanPostProcessor processor = new QueueListenerBeanPostProcessor();
		processor.setMessageListenerContainerConfiguration(customProperties);

		processor.postProcessBeanDefinitionRegistry(registry);

		Assert.assertEquals(2, registry.getBeanDefinitionCount());
		BeanDefinition definition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(definition);

		Assert.assertEquals(SimpleMessageListenerContainer.class.getName(), definition.getBeanClassName());
		Assert.assertEquals(4, definition.getPropertyValues().size());
		Assert.assertEquals("myQueue", definition.getPropertyValues().getPropertyValue("destinationName").getValue());

		BeanDefinition messageListener = (BeanDefinition) definition.getPropertyValues().getPropertyValue("messageListener").getValue();
		ConstructorArgumentValues messageListenerConstructorArgumentValues = messageListener.getConstructorArgumentValues();
		Assert.assertEquals("listenerMethod", messageListenerConstructorArgumentValues.getArgumentValue(2, String.class).getValue());

		RuntimeBeanReference messageListenerTarget = (RuntimeBeanReference) messageListenerConstructorArgumentValues.
				getArgumentValue(1, MinimalListenerConfiguration.class).getValue();
		Assert.assertEquals("queueListenerBean", messageListenerTarget.getBeanName());

		RuntimeBeanReference amazonSqs = (RuntimeBeanReference) definition.getPropertyValues().getPropertyValue("amazonSqs").getValue();
		Assert.assertEquals("customOne", amazonSqs.getBeanName());
		Assert.assertEquals(2, definition.getPropertyValues().getPropertyValue("maxNumberOfMessages").getValue());
	}

	@Test
	public void testRegisterContainerWithCustomMessageConverter() throws Exception {
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		registry.registerBeanDefinition("queueListenerBean",
				BeanDefinitionBuilder.rootBeanDefinition(CustomConverterListener.class).getBeanDefinition());
		registry.registerBeanDefinition("myMessageConverter",
				BeanDefinitionBuilder.rootBeanDefinition(ObjectMessageConverter.class).getBeanDefinition());

		QueueListenerBeanPostProcessor processor = new QueueListenerBeanPostProcessor();
		processor.postProcessBeanDefinitionRegistry(registry);

		Assert.assertEquals(3, registry.getBeanDefinitionCount());
		BeanDefinition definition = registry.getBeanDefinition(SimpleMessageListenerContainer.class.getName() + "#0");
		Assert.assertNotNull(definition);

		Assert.assertEquals(SimpleMessageListenerContainer.class.getName(), definition.getBeanClassName());


		BeanDefinition messageListener = (BeanDefinition) definition.getPropertyValues().getPropertyValue("messageListener").getValue();
		Assert.assertEquals("listenerMethod", messageListener.getConstructorArgumentValues().getArgumentValue(2, String.class).getValue());

		RuntimeBeanReference messageListenerTarget = (RuntimeBeanReference) messageListener.getConstructorArgumentValues().
				getArgumentValue(1, MinimalListenerConfiguration.class).getValue();
		Assert.assertEquals("queueListenerBean", messageListenerTarget.getBeanName());
	}

	static class MinimalListenerConfiguration {

		@QueueListener("myQueue")
		public void listenerMethod(String ignore) {
			LoggerFactory.getLogger(getClass()).debug("Method listenerMethod() called");
		}
	}

	static class CustomConverterListener {

		@QueueListener(queueName = "myQueue", messageConverter = "myMessageConverter")
		public void listenerMethod() {

		}
	}

	static class InvalidBeanWithNoDestination {

		@QueueListener
		public void methodWithNoDestination(String ignore) {
			LoggerFactory.getLogger(getClass()).debug("Method methodWithNoDestination() called");
		}
	}

	static class InvalidBeanWithAmbiguousDestinations {

		@QueueListener(value = "foo", queueName = "bar")
		public void methodWithAmbiguousDestinations(String ignore) {
			LoggerFactory.getLogger(getClass()).debug("Method methodWithAmbiguousDestinations() called");
		}
	}
}
