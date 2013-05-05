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

import org.elasticspring.messaging.config.AmazonMessagingConfigurationUtils;
import org.elasticspring.messaging.endpoint.HttpNotificationEndpointFactoryBean;
import org.elasticspring.messaging.endpoint.QueueingNotificationEndpointFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
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
public class TopicListenerBeanDefinitionRegistryPostProcessor extends AbstractMessagingBeanDefinitionRegistryPostProcessor {

	private static final String ANNOTATION_TYPE = TopicListener.class.getName();
	private String amazonSnsBeanName;
	private String amazonSqsBeanName;

	public void setAmazonSnsBeanName(String amazonSnsBeanName) {
		this.amazonSnsBeanName = amazonSnsBeanName;
	}

	public void setAmazonSqsBeanName(String amazonSqsBeanName) {
		this.amazonSqsBeanName = amazonSqsBeanName;
	}

	@Override
	protected void processBeanDefinition(BeanDefinitionRegistry registry, String beanDefinitionName, AnnotationMetadata annotationMetadata) {
		Set<MethodMetadata> methods = annotationMetadata.getAnnotatedMethods(ANNOTATION_TYPE);
		for (MethodMetadata method : methods) {
			BeanDefinition definition = getEndPointDefinition(beanDefinitionName, method, registry);
			String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
			registry.registerBeanDefinition(beanName, definition);
		}
	}

	private BeanDefinition getEndPointDefinition(String beanDefinitionName, MethodMetadata method, BeanDefinitionRegistry beanDefinitionRegistry) {
		TopicListener.NotificationProtocol protocol = (TopicListener.NotificationProtocol) method.getAnnotationAttributes(ANNOTATION_TYPE).get("protocol");
		BeanDefinitionBuilder beanDefinitionBuilder;

		switch (protocol) {
			case HTTP:
			case HTTPS:
				beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HttpNotificationEndpointFactoryBean.class);
				beanDefinitionBuilder.addConstructorArgReference(getAmazonSnsBeanName(beanDefinitionRegistry));
				break;
			case SQS:
				beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueingNotificationEndpointFactoryBean.class);
				beanDefinitionBuilder.addConstructorArgReference(getAmazonSnsBeanName(beanDefinitionRegistry));
				beanDefinitionBuilder.addConstructorArgReference(getAmazonSqsBeanName(beanDefinitionRegistry));
				break;
			default:
				throw new IllegalArgumentException("The protocol :'" + protocol + "' is currently not supported");
		}
		beanDefinitionBuilder.addConstructorArgValue(method.getAnnotationAttributes(ANNOTATION_TYPE).get("topicName"));
		beanDefinitionBuilder.addConstructorArgValue(method.getAnnotationAttributes(ANNOTATION_TYPE).get("protocol"));
		beanDefinitionBuilder.addConstructorArgValue(method.getAnnotationAttributes(ANNOTATION_TYPE).get("endpoint"));
		beanDefinitionBuilder.addConstructorArgReference(beanDefinitionName);
		beanDefinitionBuilder.addConstructorArgValue(method.getMethodName());
		return beanDefinitionBuilder.getBeanDefinition();
	}

	private String getAmazonSnsBeanName(BeanDefinitionRegistry beanDefinitionRegistry) {
		if (StringUtils.hasText(this.amazonSnsBeanName)) {
			return this.amazonSnsBeanName;
		}

		BeanDefinitionHolder definitionHolder = AmazonMessagingConfigurationUtils.
				registerAmazonSnsClient(beanDefinitionRegistry, this);
		return definitionHolder.getBeanName();
	}

	private String getAmazonSqsBeanName(BeanDefinitionRegistry beanDefinitionRegistry) {
		if (StringUtils.hasText(this.amazonSqsBeanName)) {
			return this.amazonSqsBeanName;
		}

		BeanDefinitionHolder definitionHolder = AmazonMessagingConfigurationUtils.
				registerAmazonSqsClient(beanDefinitionRegistry, this);
		return definitionHolder.getBeanName();
	}
}
