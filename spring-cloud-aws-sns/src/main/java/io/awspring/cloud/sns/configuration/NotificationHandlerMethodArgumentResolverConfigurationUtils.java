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
package io.awspring.cloud.sns.configuration;

import io.awspring.cloud.sns.handlers.NotificationMessageHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.NotificationStatusHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.NotificationSubjectHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Simple util class that is used to create handlers for Http/s notification support.
 *
 * @author Alain Sahli
 * @since 1.0
 */
public final class NotificationHandlerMethodArgumentResolverConfigurationUtils {

	private NotificationHandlerMethodArgumentResolverConfigurationUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static HandlerMethodArgumentResolver getNotificationHandlerMethodArgumentResolver(SnsClient snsClient) {
		HandlerMethodArgumentResolverComposite composite = new HandlerMethodArgumentResolverComposite();
		composite.addResolver(new NotificationStatusHandlerMethodArgumentResolver(snsClient));
		composite.addResolver(new NotificationMessageHandlerMethodArgumentResolver());
		composite.addResolver(new NotificationSubjectHandlerMethodArgumentResolver());
		return composite;
	}

}
