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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.messaging.support.converter.SimpleMessageConverter;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} that inspects bean definition
 * for {@link QueueListener} annotation. Creates the necessary messaging listener to enable message listening for the
 * annotated method.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanDefinitionRegistryPostProcessor extends AbstractMessagingBeanDefinitionRegistryPostProcessor {

	/**
	 * The annotation type which will be used to inspect
	 */
	private static final String ANNOTATION_TYPE = QueueListener.class.getName();

	/**
	 * Annotation attributes for the {@link #ANNOTATION_TYPE} annotation
	 */
	private static final String DESTINATION_ATTRIBUTE_NAME = "queueName";
	private static final String VALUE_ATTRIBUTE_NAME = "value";
	private static final String MESSAGE_CONVERTER_ATTRIBUTE_NAME = "messageConverter";
	private String parentBeanName;

	/**
	 * Configures the parent bean name used as a template to create the message listeners. The parent bean definition must
	 * refer to an existing abstract or non-abstract bean which already contains the basic configuration for the message
	 * listener. This ae the main collaborators (e.g. Amazon SDK) as well as application wide configuration settings which
	 * are not configurable inside the annotation itself.
	 *
	 * @param parentBeanName
	 * 		- a bean name to an existing bean which will be used as a parent to create a message listener. The bean must be
	 * 		of
	 * 		type {@link org.elasticspring.messaging.listener.AbstractMessageListenerContainer}
	 */
	public void setParentBeanName(String parentBeanName) {
		this.parentBeanName = parentBeanName;
	}

	/**
	 * Processes all the methods which contain a {@link #ANNOTATION_TYPE} annotation and create a message listener for
	 * them.
	 *
	 * @param registry
	 * 		- the bean definition registry which may be used to inspect bean definition or register new ones
	 * @param beanDefinitionName
	 * 		-  the name of bean definition for which the {@link AnnotationMetadata} are provided
	 * @param annotationMetadata
	 * 		- the annotation meta data which is inspected
	 */
	@Override
	protected void processBeanDefinition(BeanDefinitionRegistry registry, String beanDefinitionName, AnnotationMetadata annotationMetadata) {
		Set<MethodMetadata> methods = annotationMetadata.getAnnotatedMethods(ANNOTATION_TYPE);
		for (MethodMetadata method : methods) {
			BeanDefinition definition = getContainerBeanDefinition(beanDefinitionName, method);
			String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
			registry.registerBeanDefinition(beanName, definition);
		}
	}

	/**
	 * Returns the bean definition for the container. The bean definition will use the {@link #parentBeanName} and
	 * configure the destination name and message listener for the bean definition.
	 *
	 * @param beanName
	 * 		- the bean name for which the message endpoint will be registered.
	 * @param methodMetadata
	 * 		- the method meta data used to retrieve the method metadata.
	 * @return - a fully configured bean definition containing the container and the message listener configuration
	 */
	private BeanDefinition getContainerBeanDefinition(String beanName, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.childBeanDefinition(this.parentBeanName);
		builder.addPropertyValue("destinationName", getDestinationName(methodMetadata));
		builder.addPropertyValue("messageListener", getMessageListenerBeanDefinition(beanName, methodMetadata));
		return builder.getBeanDefinition();
	}

	/**
	 * Created a {@link MessageListenerAdapter} bean definition containing the bean definition as the target instance, and
	 * the method as well as message converter configuration.
	 *
	 * @param beanName
	 * 		- the bean name for which the listener is created
	 * @param methodMetadata
	 * 		-  the method meta data used for the destination and the message converter
	 * @return the message listener definition used in the container
	 */
	private BeanDefinition getMessageListenerBeanDefinition(String beanName, MethodMetadata methodMetadata) {
		BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.rootBeanDefinition(MessageListenerAdapter.class);

		if (StringUtils.hasText(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, MESSAGE_CONVERTER_ATTRIBUTE_NAME))) {
			listenerBuilder.addConstructorArgValue(new RuntimeBeanReference(getNullSafeAnnotationAttribute(ANNOTATION_TYPE, methodMetadata, MESSAGE_CONVERTER_ATTRIBUTE_NAME)));
		} else {
			listenerBuilder.addConstructorArgValue(BeanDefinitionBuilder.rootBeanDefinition(SimpleMessageConverter.class).getBeanDefinition());
		}

		listenerBuilder.addConstructorArgReference(beanName);
		listenerBuilder.addConstructorArgValue(methodMetadata.getMethodName());
		return listenerBuilder.getBeanDefinition();
	}

	/**
	 * Resolves the destination name and check for incompatible configuration like missing or ambiguous configuration
	 * setting.
	 *
	 * @param methodMetadata - the method meta data that contains the destination information
	 * @return the destination name configuration in the annotation
	 * @throws IllegalStateException if the destination name does not exist or exist multiple times.
	 */
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

	/**
	 * Utility method to simplify the retrieval of annotation attributes that may exist.
	 * @param annotationType - the type of the annotation
	 * @param methodMetadata - the meta data containing the annotation metadata
	 * @param attributeName -  the name of the annotation attribute which should be retrieved
	 * @return returns the annotation attributes value as String or null if it does not exist.
	 */
	private static String getNullSafeAnnotationAttribute(String annotationType, MethodMetadata methodMetadata, String attributeName) {
		if (methodMetadata.getAnnotationAttributes(annotationType).containsKey(attributeName)) {
			return methodMetadata.getAnnotationAttributes(annotationType).get(attributeName).toString();
		} else {
			return null;
		}
	}
}