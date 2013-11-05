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

package org.elasticspring.messaging.listener;

import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationEndpointMessageListenerAdapter extends MessageListenerAdapter {

	public NotificationEndpointMessageListenerAdapter(Object delegate, String listenerMethod) {
		super(new NotificationMessageConverter(), delegate, listenerMethod);
	}

	@Override
	protected void prepareArguments(MethodInvoker methodInvoker, Object payload) {
		NotificationMessageConverter.NotificationMessage notificationMessage = (NotificationMessageConverter.NotificationMessage) payload;

		if (ClassUtils.hasMethod(AopUtils.getTargetClass(getDelegate()), getListenerMethod(), String.class, String.class)) {
			methodInvoker.setArguments(new Object[]{notificationMessage.getBody(), notificationMessage.getSubject()});
		} else {
			methodInvoker.setArguments(new Object[]{notificationMessage.getBody()});
		}
	}
}