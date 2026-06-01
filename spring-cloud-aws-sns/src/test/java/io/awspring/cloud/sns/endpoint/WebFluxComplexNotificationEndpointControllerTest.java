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
package io.awspring.cloud.sns.endpoint;

import static io.awspring.cloud.sns.configuration.WebFluxNotificationHandlerMethodArgumentResolverConfigurationUtils.getWebFluxNotificationMessageHandlerMethodArgumentResolver;
import static io.awspring.cloud.sns.configuration.WebFluxNotificationHandlerMethodArgumentResolverConfigurationUtils.getWebFluxNotificationStatusHandlerMethodArgumentResolver;
import static io.awspring.cloud.sns.configuration.WebFluxNotificationHandlerMethodArgumentResolverConfigurationUtils.getWebFluxNotificationSubjectHandlerMethodArgumentResolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sns.annotation.endpoint.NotificationMessageMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationSubscriptionMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationUnsubscribeConfirmationMapping;
import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 * @author Matej Nedic
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebFluxComplexNotificationEndpointControllerTest.Config.class)
class WebFluxComplexNotificationEndpointControllerTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SnsClient snsClient;

	@Autowired
	private SnsMessageManager snsMessageManager;

	@Autowired
	private WebFluxComplexNotificationTestController notificationTestController;

	@Nested
	class SubscriptionConfirmationRequestReceived {

		@Test
		void subscriptionConfirmedThroughSubscriptionStatus() throws Exception {
			// Arrange
			byte[] subscriptionRequestJsonContent = FileCopyUtils
					.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();

			// Act
			client.post().uri("/myComplexTopic").header("x-amz-sns-message-type", "SubscriptionConfirmation")
					.contentType(MediaType.APPLICATION_JSON).bodyValue(subscriptionRequestJsonContent).exchange()
					.expectStatus().isNoContent();

			// Assert
			verify(snsClient, times(1)).confirmSubscription(
					ConfirmSubscriptionRequest.builder().topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
							.token("111111111111111111111111111111111111111111111111111111"
									+ "1111111111111111111111111111111111111111111111111111"
									+ "1111111111111111111111111111111111111111111111111111"
									+ "1111111111111111111111111111111111111111111111111111")
							.build());
		}

	}

	@Nested
	class NotificationReceivedAsMessageWithComplextContent {

		@Test
		void notificationSubjectAndMessagePassedToAnnotatedControllerMethod() throws Exception {
			// Arrange
			byte[] notificationJsonContent = FileCopyUtils.copyToByteArray(
					getClass().getClassLoader().getResourceAsStream("notificationMessage-complexObject.json"));

			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();

			// Act
			client.post().uri("/myComplexTopic").header("x-amz-sns-message-type", "Notification")
					.contentType(MediaType.APPLICATION_JSON).bodyValue(notificationJsonContent).exchange()
					.expectStatus().isNoContent();

			// Assert
			assertThat(notificationTestController.getMessage().getFirstName()).isEqualTo("Agim");
			assertThat(notificationTestController.getMessage().getLastName()).isEqualTo("Emruli");
			assertThat(notificationTestController.getSubject()).isEqualTo("Notification Subject");
		}

		@Test
		void notificationSubjectAndMessagePassedToAnnotatedControllerMethod_Check_UTF8() throws Exception {
			// Arrange
			byte[] notificationJsonContent = FileCopyUtils.copyToByteArray(getClass().getClassLoader()
					.getResourceAsStream("notificationMessage-complexObject-UTF-8-Check.json"));

			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();

			// Act
			client.post().uri("/myComplexTopic").header("x-amz-sns-message-type", "Notification")
					.contentType(MediaType.APPLICATION_JSON).bodyValue(notificationJsonContent).exchange()
					.expectStatus().isNoContent();

			// Assert
			assertThat(notificationTestController.getMessage().getFirstName()).isEqualTo("الْحُرُوف");
			assertThat(notificationTestController.getMessage().getLastName()).isEqualTo("口廿竹十火");
			assertThat(notificationTestController.getSubject()).isEqualTo("Notification Subject");
		}

	}

	@Nested
	class UnsubscribeConfirmationReceivedAsMessage {

		@Test
		void reSubscriptionCalledByController() throws Exception {
			// Arrange
			byte[] notificationJsonContent = FileCopyUtils
					.copyToByteArray(getClass().getClassLoader().getResourceAsStream("unsubscribeConfirmation.json"));

			WebTestClient client = WebTestClient.bindToApplicationContext(context).build();

			// Act
			client.post().uri("/myComplexTopic").header("x-amz-sns-message-type", "UnsubscribeConfirmation")
					.contentType(MediaType.APPLICATION_JSON).bodyValue(notificationJsonContent).exchange()
					.expectStatus().isNoContent();

			// Assert
			verify(snsClient, times(1)).confirmSubscription(
					ConfirmSubscriptionRequest.builder().topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
							.token("2336412f37fb687f5d51e6e241d638b05824e9e2f6713b42abaeb86"
									+ "07743f5ba91d34edd2b9dabe2f1616ed77c0f8801ee79911d34dc"
									+ "a3d210c228af87bd5d9597bf0d6093a1464e03af6650e992ecf546"
									+ "05e020f04ad3d47796045c9f24d902e72e811a1ad59852cad453f4" + "0bddfb45")
							.build());
		}

	}

	@EnableWebFlux
	@Import(WebFluxComplexNotificationTestController.class)
	static class Config {

		@Bean
		SnsClient snsClient() {
			return Mockito.mock(SnsClient.class);
		}

		@Bean
		SnsMessageManager snsMessageManager() {
			return Mockito.mock(SnsMessageManager.class);
		}

		@Bean
		public WebFluxConfigurer snsWebFluxConfigurer(SnsClient snsClient, SnsMessageManager snsMessageManager) {
			return new WebFluxConfigurer() {
				@Override
				public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
					configurer.addCustomResolver(
							getWebFluxNotificationStatusHandlerMethodArgumentResolver(snsClient, snsMessageManager),
							getWebFluxNotificationMessageHandlerMethodArgumentResolver(snsMessageManager),
							getWebFluxNotificationSubjectHandlerMethodArgumentResolver());
				}
			};
		}

	}

	@Controller
	@RequestMapping("/myComplexTopic")
	static class WebFluxComplexNotificationTestController {

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
			status.confirmSubscription();
		}

		@NotificationMessageMapping
		void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage Person message) {
			this.subject = subject;
			this.message = message;
		}

		@NotificationUnsubscribeConfirmationMapping
		void handleUnsubscribeMessage(NotificationStatus status) {
			status.confirmSubscription();
		}

	}

	static class Person {

		private String firstName;

		private String lastName;

		String getFirstName() {
			return this.firstName;
		}

		void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		String getLastName() {
			return this.lastName;
		}

		void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

}
