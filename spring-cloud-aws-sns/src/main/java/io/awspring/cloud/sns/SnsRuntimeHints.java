/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sns;

import io.awspring.cloud.sns.handlers.NotificationMessageHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import io.awspring.cloud.sns.handlers.NotificationStatusHandlerMethodArgumentResolver;
import io.awspring.cloud.sns.handlers.NotificationSubjectHandlerMethodArgumentResolver;
import java.util.stream.Stream;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

public class SnsRuntimeHints implements RuntimeHintsRegistrar {
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		Stream.of(NotificationStatusHandlerMethodArgumentResolver.class,
				NotificationStatusHandlerMethodArgumentResolver.AmazonSnsNotificationStatus.class,
				NotificationMessageHandlerMethodArgumentResolver.class,
				NotificationMessageHandlerMethodArgumentResolver.ByteArrayHttpInputMessage.class,
				NotificationSubjectHandlerMethodArgumentResolver.class, NotificationStatus.class)
				.forEach(type -> hints.reflection().registerType(TypeReference.of(type),
						hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
								MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS)));
	}
}
