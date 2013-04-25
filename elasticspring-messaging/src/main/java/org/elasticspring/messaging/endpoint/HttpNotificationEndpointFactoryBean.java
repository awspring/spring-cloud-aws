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

package org.elasticspring.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.Subscription;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class HttpNotificationEndpointFactoryBean extends AbstractNotificationEndpointFactoryBean<Object> implements ServletContextAware, BeanNameAware {

	private final NotificationMessageConverter messageConverter = new NotificationMessageConverter();
	private ServletContext servletContext;
	private String beanName;


	public HttpNotificationEndpointFactoryBean(AmazonSNS amazonSns, String topicName,
											   TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
		super(amazonSns, topicName, protocol, endpoint, target, method);
		Assert.isTrue(protocol == TopicListener.NotificationProtocol.HTTP || protocol == TopicListener.NotificationProtocol.HTTPS,
				"This endpoint only support http and https endpoints");
	}

	@Override
	protected Object doCreateEndpointInstance(Subscription subscription) {
		NotificationEndpointHttpRequestHandler requestHandler = new NotificationEndpointHttpRequestHandler(getAmazonSns(),
				this.messageConverter, getTarget(), getMethod(), getEndpoint(), subscription.getTopicArn());

		requestHandler.setServletContext(this.servletContext);
		requestHandler.setBeanName(this.beanName);
		return requestHandler;
	}

	@Override
	public Class<NotificationEndpointHttpRequestHandler> getObjectType() {
		return NotificationEndpointHttpRequestHandler.class;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}
}