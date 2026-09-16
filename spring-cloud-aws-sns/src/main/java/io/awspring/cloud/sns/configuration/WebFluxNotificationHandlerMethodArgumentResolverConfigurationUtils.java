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
package io.awspring.cloud.sns.configuration;

import io.awspring.cloud.sns.handlers.webflux.WebFluxNotificationMessageHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.webflux.WebFluxNotificationStatusHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.webflux.WebFluxNotificationSubjectHandlerMethodArgumentResolver;
import org.jspecify.annotations.Nullable;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Utility class for creating WebFlux {@link HandlerMethodArgumentResolver} instances for SNS HTTP/S notification
 * endpoint support. This is the reactive counterpart of
 * {@link NotificationHandlerMethodArgumentResolverConfigurationUtils}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class WebFluxNotificationHandlerMethodArgumentResolverConfigurationUtils {
	public static HandlerMethodArgumentResolver getWebFluxNotificationStatusHandlerMethodArgumentResolver(
			SnsClient snsClient, @Nullable SnsMessageManager snsMessageManager) {
		return snsMessageManager != null
				? new WebFluxNotificationStatusHandlerMethodArgumentResolver(snsClient, snsMessageManager)
				: new WebFluxNotificationStatusHandlerMethodArgumentResolver(snsClient);
	}

	public static HandlerMethodArgumentResolver getWebFluxNotificationMessageHandlerMethodArgumentResolver(
			@Nullable SnsMessageManager snsMessageManager) {
		return snsMessageManager != null
				? new WebFluxNotificationMessageHandlerMethodArgumentResolver(
						WebFluxNotificationMessageHandlerMethodArgumentResolver.defaultDecoders, snsMessageManager)
				: new WebFluxNotificationMessageHandlerMethodArgumentResolver(
						WebFluxNotificationMessageHandlerMethodArgumentResolver.defaultDecoders);
	}

	public static HandlerMethodArgumentResolver getWebFluxNotificationSubjectHandlerMethodArgumentResolver() {
		return new WebFluxNotificationSubjectHandlerMethodArgumentResolver();
	}
}
