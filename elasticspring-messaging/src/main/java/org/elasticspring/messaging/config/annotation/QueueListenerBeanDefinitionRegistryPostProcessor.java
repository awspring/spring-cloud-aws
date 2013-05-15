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

import org.elasticspring.messaging.listener.MessageListenerAdapter;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanDefinitionRegistryPostProcessor extends AbstractMessagingBeanDefinitionRegistryPostProcessor {

	private static final String ANNOTATION_TYPE = QueueListener.class.getName();
	private static final String DESTINATION_ATTRIBUTE_NAME = "queueName";
	private static final String VALUE_ATTRIBUTE_NAME = "value";
	private static final String MESSAGE_CONVERTER_ATTRIBUTE_NAME = "messageConverter";
	private String parentBeanName;

	public void setParentBeanName(String parentBeanName) {
		this.parentBeanName = parentBeanName;
	}

	@Override
	protected void processBeanDefinition(BeanDefinitionRegistry registry, String beanDefinitionName, AnnotationMetadata annotationMetadata) {
		Set<MethodMetadata> methods = annotationMetadata.getAnnotatedMethods(ANNOTATION_TYPE);
		for (MethodMetadata method : methods) {
			BeanDefinition definition = getContainerBeanDefinition(beanDefinitionName, method);
			String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
			registry.registerBeanDefinition(beanName, definition);
		}
	}

	private BeanDefinition getContainerBeanDefinition(String beanName, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.childBeanDefinition(this.parentBeanName);
		builder.addPropertyValue("destinationName", getDestinationName(methodMetadata));
		builder.addPropertyValue("messageListener", getMessageListenerBeanDefinition(beanName, methodMetadata));
		return builder.getBeanDefinition();
	}

	private BeanDefinition getMessageListenerBeanDefinition(String target, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.rootBeanDefinition(MessageListenerAdapter.class);

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, MESSAGE_CONVERTER_ATTRIBUTE_NAME))) {
			listenerBuilder.addConstructorArgValue(new RuntimeBeanReference(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, MESSAGE_CONVERTER_ATTRIBUTE_NAME)));
		} else {
			listenerBuilder.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(StringMessageConverter.class).getBeanDefinition());
		}

		listenerBuilder.addConstructorArgReference(target);
		listenerBuilder.addConstructorArgValue(methodMetadata.getMethodName());
		return listenerBuilder.getBeanDefinition();
	}


	private static String getDestinationName(MethodMetadata methodMetadata) {
		if (StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, VALUE_ATTRIBUTE_NAME)) &&
				StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, DESTINATION_ATTRIBUTE_NAME))) {
			throw new IllegalStateException("The annotation " + ANNOTATION_TYPE + " on type:'" + methodMetadata.getDeclaringClassName() + "' and method:'"
					+ methodMetadata.getMethodName() + "must contain either " + VALUE_ATTRIBUTE_NAME + " or " + DESTINATION_ATTRIBUTE_NAME + " attribute but not both!");
		}

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, VALUE_ATTRIBUTE_NAME))) {
			return getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, VALUE_ATTRIBUTE_NAME);
		}

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, DESTINATION_ATTRIBUTE_NAME))) {
			return getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, DESTINATION_ATTRIBUTE_NAME);
		}

		throw new IllegalStateException("The annotation " + ANNOTATION_TYPE + " on type:'" + methodMetadata.getDeclaringClassName() + "' and method:'"
				+ methodMetadata.getMethodName() + "' must contain either " + VALUE_ATTRIBUTE_NAME + " or " + DESTINATION_ATTRIBUTE_NAME + " attribute!");
	}

	private static String getNullSafeAnnotationAttribute(String annotationType, MethodMetadata methodMetadata, String attributeName) {
		if (methodMetadata.getAnnotationAttributes(annotationType).containsKey(attributeName)) {
			return methodMetadata.getAnnotationAttributes(annotationType).get(attributeName).toString();
		} else {
			return null;
		}
	}
}