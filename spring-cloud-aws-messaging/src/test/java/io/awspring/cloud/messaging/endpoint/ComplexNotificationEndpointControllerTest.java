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

package org.springframework.cloud.aws.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSns;
import org.springframework.cloud.aws.messaging.endpoint.ComplexNotificationEndpointControllerTest.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Agim Emruli
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = Config.class)
class ComplexNotificationEndpointControllerTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private AmazonSNS amazonSnsMock;

	@Autowired
	private ComplexNotificationTestController notificationTestController;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void subscribe_subscriptionConfirmationRequestReceived_subscriptionConfirmedThroughSubscriptionStatus()
			throws Exception {
		// Arrange
		byte[] subscriptionRequestJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("subscriptionConfirmation.json", getClass()).getInputStream());

		// Act
		this.mockMvc.perform(post("/myComplexTopic").header("x-amz-sns-message-type", "SubscriptionConfirmation")
				.content(subscriptionRequestJsonContent)).andExpect(status().isNoContent());

		// Assert
		verify(this.amazonSnsMock, times(1)).confirmSubscription("arn:aws:sns:eu-west-1:111111111111:mySampleTopic",
				"111111111111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111111111111");
	}

	// @checkstyle:off
	@Test
	void notification_notificationReceivedAsMessageWithComplexContent_notificationSubjectAndMessagePassedToAnnotatedControllerMethod()
			throws Exception {
		// @checkstyle:on
		// Arrange
		byte[] notificationJsonContent = FileCopyUtils.copyToByteArray(
				new ClassPathResource("notificationMessage-complexObject.json", getClass()).getInputStream());

		// Act
		this.mockMvc.perform(post("/myComplexTopic").header("x-amz-sns-message-type", "Notification")
				.content(notificationJsonContent)).andExpect(status().isNoContent());

		// Assert
		assertThat(this.notificationTestController.getMessage().getFirstName()).isEqualTo("Agim");
		assertThat(this.notificationTestController.getMessage().getLastName()).isEqualTo("Emruli");
		assertThat(this.notificationTestController.getSubject()).isEqualTo("Notification Subject");
	}

	// @checkstyle:off
	@Test
	void notification_notificationReceivedAsMessageWithComplexContent_notificationSubjectAndMessagePassedToAnnotatedControllerMethod_Check_UTF8()
			throws Exception {
		// @checkstyle:on
		// Arrange
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("notificationMessage-complexObject-UTF-8-Check.json", getClass())
						.getInputStream());

		// Act
		this.mockMvc.perform(post("/myComplexTopic").header("x-amz-sns-message-type", "Notification")
				.content(notificationJsonContent)).andExpect(status().isNoContent());

		// Assert
		assertThat(this.notificationTestController.getMessage().getFirstName()).isEqualTo("الْحُرُوف");
		assertThat(this.notificationTestController.getMessage().getLastName()).isEqualTo("口廿竹十火");
		assertThat(this.notificationTestController.getSubject()).isEqualTo("Notification Subject");
	}

	@Test
	void notification_unsubscribeConfirmationReceivedAsMessage_reSubscriptionCalledByController() throws Exception {
		// Arrange
		byte[] notificationJsonContent = FileCopyUtils
				.copyToByteArray(new ClassPathResource("unsubscribeConfirmation.json", getClass()).getInputStream());

		// Act
		this.mockMvc.perform(post("/myComplexTopic").header("x-amz-sns-message-type", "UnsubscribeConfirmation")
				.content(notificationJsonContent)).andExpect(status().isNoContent());

		// Assert
		verify(this.amazonSnsMock, times(1)).confirmSubscription("arn:aws:sns:eu-west-1:111111111111:mySampleTopic",
				"2336412f37fb687f5d51e6e241d638b05824e9e2f6713b42abaeb86"
						+ "07743f5ba91d34edd2b9dabe2f1616ed77c0f8801ee79911d34dc"
						+ "a3d210c228af87bd5d9597bf0d6093a1464e03af6650e992ecf546"
						+ "05e020f04ad3d47796045c9f24d902e72e811a1ad59852cad453f4" + "0bddfb45");
	}

	@EnableWebMvc
	@EnableSns
	@Import(ComplexNotificationTestController.class)
	static class Config {

		@Bean
		AmazonSNS amazonSNS() {
			return Mockito.mock(AmazonSNS.class);
		}

	}

}
