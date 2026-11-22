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
package io.awspring.cloud.sqs.support.resolver;

import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

/**
 * Resolves listener method parameters of type {@link S3EventNotification} from the body of an S3 event notification
 * that S3 sends directly to an SQS queue.
 * <p>
 * This resolver parses the body with {@link S3EventNotification#fromJson(String)}, because Jackson cannot deserialize
 * the AWS SDK v2 model. S3 sends an {@code s3:TestEvent} message when a notification is configured. That message has no
 * records, so this resolver returns a notification with an empty record list for it.
 * <p>
 * {@link io.awspring.cloud.sqs.annotation.SqsListenerAnnotationBeanPostProcessor} registers this resolver when
 * {@code software.amazon.awssdk:s3-event-notifications} is on the classpath.
 *
 * @author Kassa
 * @since 4.3.0
 */
public class S3EventNotificationArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return S3EventNotification.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		S3EventNotification notification = S3EventNotification.fromJson(message.getPayload().toString());
		return notification.getRecords() != null ? notification : new S3EventNotification(List.of());
	}

}
