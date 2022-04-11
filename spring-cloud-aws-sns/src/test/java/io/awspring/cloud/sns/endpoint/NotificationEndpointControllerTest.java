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

import static io.awspring.cloud.sns.configuration.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.awspring.cloud.sns.annotation.endpoint.NotificationMessageMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationSubscriptionMapping;
import io.awspring.cloud.sns.annotation.endpoint.NotificationUnsubscribeConfirmationMapping;
import io.awspring.cloud.sns.annotation.handlers.NotificationMessage;
import io.awspring.cloud.sns.annotation.handlers.NotificationSubject;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 * @author Agim Emruli
 * @author Matej Nedic
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = NotificationEndpointControllerTest.Config.class)
class NotificationEndpointControllerTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private SnsClient snsClient;

	@Autowired
	private NotificationTestController notificationTestController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void subscribe_subscriptionConfirmationRequestReceived_subscriptionConfirmedThroughSubscriptionStatus()
			throws Exception {
		// Arrange
		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		// Act
		this.mockMvc.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "SubscriptionConfirmation")
				.content(subscriptionRequestJsonContent)).andExpect(status().isNoContent());

		// Assert
		verify(this.snsClient, times(1)).confirmSubscription(
				ConfirmSubscriptionRequest.builder().topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
						.token("111111111111111111111111111111111111111111111111111111"
								+ "11111111111111111111111111111111111111111111111111111"
								+ "111111111111111111111111111111111111111111111111111111"
								+ "1111111111111111111111111111111111111111111111111")
						.build());
	}

	@Test
	void notification_notificationReceivedAsMessage_notificationSubjectAndMessagePassedToAnnotatedControllerMethod()
			throws Exception {
		// Arrange
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));

		// Act
		this.mockMvc.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "Notification")
				.content(notificationJsonContent)).andExpect(status().isNoContent());

		// Assert
		assertThat(this.notificationTestController.getMessage()).isEqualTo("asdasd");
		assertThat(this.notificationTestController.getSubject()).isEqualTo("asdasd");
	}

	@Test
	void notification_unsubscribeConfirmationReceivedAsMessage_reSubscriptionCalledByController() throws Exception {
		// Arrange
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("unsubscribeConfirmation.json"));

		// Act
		this.mockMvc.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "UnsubscribeConfirmation")
				.content(notificationJsonContent)).andExpect(status().isNoContent());

		// Assert
		verify(this.snsClient, times(1)).confirmSubscription(
				ConfirmSubscriptionRequest.builder().topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
						.token("2336412f37fb687f5d51e6e241d638b05824e9e2f6713b42abaeb"
								+ "8607743f5ba91d34edd2b9dabe2f1616ed77c0f8801ee79911d3"
								+ "4dca3d210c228af87bd5d9597bf0d6093a1464e03af6650e992ecf"
								+ "54605e020f04ad3d47796045c9f24d902e72e811a1ad59852cad453f40bddfb45")
						.build());
	}

	@EnableWebMvc
	@Import(NotificationTestController.class)
	static class Config {

		@Bean
		SnsClient snsClient() {
			return Mockito.mock(SnsClient.class);
		}

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(SnsClient snsClient) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
					argumentResolvers.add(getNotificationHandlerMethodArgumentResolver(snsClient));
				}
			};
		}

	}

	@Controller
	@RequestMapping("/mySampleTopic")
	static class NotificationTestController {

		private String subject;

		private String message;

		String getSubject() {
			return this.subject;
		}

		String getMessage() {
			return this.message;
		}

		@NotificationSubscriptionMapping
		void handleSubscriptionMessage(NotificationStatus status) throws IOException {
			// We subscribe to start receive the message
			status.confirmSubscription();
		}

		@NotificationMessageMapping
		void handleNotificationMessage(@NotificationSubject String subject, @NotificationMessage String message) {
			this.subject = subject;
			this.message = message;
		}

		@NotificationUnsubscribeConfirmationMapping
		void handleUnsubscribeMessage(NotificationStatus status) {
			// e.g. the client has been unsubscribed and we want to "re-subscribe"
			status.confirmSubscription();
		}

	}

}
