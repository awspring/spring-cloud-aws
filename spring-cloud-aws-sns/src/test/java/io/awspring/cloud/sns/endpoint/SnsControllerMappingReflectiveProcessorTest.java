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
package io.awspring.cloud.sns.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sns.annotation.endpoint.NotificationMessageMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationSubscriptionMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationUnsubscribeConfirmationMapping;
import io.awspring.cloud.sns.annotation.endpoint.SnsControllerMappingReflectiveProcessor;
import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class SnsControllerMappingReflectiveProcessorTest {

	private final SnsControllerMappingReflectiveProcessor processor = new SnsControllerMappingReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerReflectiveHintsForMethodHandleSubscribe() throws NoSuchMethodException {
		Method method = ComplexNotificationTestController.class.getDeclaredMethod("handleSubscriptionMessage",
				NotificationStatus.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(typeHint -> assertThat(typeHint.getType())
				.isEqualTo(TypeReference.of(ComplexNotificationTestController.class)), typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(NotificationStatus.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.ACCESS_DECLARED_FIELDS);
				});
	}

	@Test
	void registerReflectiveHintsForMethodHandleNotificationMessage() throws NoSuchMethodException {
		Method method = ComplexNotificationTestController.class.getDeclaredMethod("handleNotificationMessage",
				String.class, Person.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(typeHint -> assertThat(typeHint.getType())
				.isEqualTo(TypeReference.of(ComplexNotificationTestController.class)), typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
				}, typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Person.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.ACCESS_DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("getFirstName"),
							hint -> assertThat(hint.getName()).isEqualTo("setFirstName"),
							hint -> assertThat(hint.getName()).isEqualTo("getLastName"),
							hint -> assertThat(hint.getName()).isEqualTo("setLastName"));
				});
	}

	@Test
	void registerReflectiveHintsForMethodHandleUnsubscribe() throws NoSuchMethodException {
		Method method = ComplexNotificationTestController.class.getDeclaredMethod("handleUnsubscribeMessage",
				NotificationStatus.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(typeHint -> assertThat(typeHint.getType())
				.isEqualTo(TypeReference.of(ComplexNotificationTestController.class)), typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(NotificationStatus.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.ACCESS_DECLARED_FIELDS);
				});
	}

	@Controller
	@RequestMapping("/myComplexTopic")
	static class ComplexNotificationTestController {

		private String subject;

		private Person message;

		String getSubject() {
			return this.subject;
		}

		Person getMessage() {
			return this.message;
		}

		@NotificationSubscriptionMapping
		void handleSubscriptionMessage(NotificationStatus status) throws IOException {
			// We subscribe to start receive the message
			status.confirmSubscription();
		}

		@NotificationMessageMapping
		void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage Person message) {
			this.subject = subject;
			this.message = message;
		}

		@NotificationUnsubscribeConfirmationMapping
		void handleUnsubscribeMessage(NotificationStatus status) {
			// e.g. the client has been unsubscribed and we want to "re-subscribe"
			status.confirmSubscription();
		}
	}

	static class Person {

		private String firstName;

		private String lastName;

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

}
