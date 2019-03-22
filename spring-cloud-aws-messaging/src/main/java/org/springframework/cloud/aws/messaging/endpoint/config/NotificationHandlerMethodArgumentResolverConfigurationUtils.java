/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.endpoint.config;

import com.amazonaws.services.sns.AmazonSNS;

import org.springframework.cloud.aws.messaging.endpoint.NotificationMessageHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationSubjectHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public final class NotificationHandlerMethodArgumentResolverConfigurationUtils {

	private NotificationHandlerMethodArgumentResolverConfigurationUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static HandlerMethodArgumentResolver getNotificationHandlerMethodArgumentResolver(
			AmazonSNS amazonSns) {
		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(
				new NotificationStatusHandlerMethodArgumentResolver(amazonSns));
		composite.addResolver(new NotificationMessageHandlerMethodArgumentResolver());
		composite.addResolver(new NotificationSubjectHandlerMethodArgumentResolver());
		return composite;
	}

}
