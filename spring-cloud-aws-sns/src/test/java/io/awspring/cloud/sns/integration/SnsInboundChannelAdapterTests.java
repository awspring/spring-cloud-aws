/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sns.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.awspring.cloud.sns.core.SnsHeaders;
import io.awspring.cloud.sns.handlers.NotificationStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StreamUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.WebApplicationContext;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;

/**
 * @author Artem Bilan
 * @author Kamil Przerwa
 *
 * @since 4.0
 */
@SpringJUnitWebConfig
@DirtiesContext
class SnsInboundChannelAdapterTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private SnsClient amazonSns;

	@Autowired
	private PollableChannel inputChannel;

	@Value("classpath:subscriptionConfirmation.json")
	private Resource subscriptionConfirmation;

	@Value("classpath:notificationMessage.json")
	private Resource notificationMessage;

	@Value("classpath:unsubscribeConfirmation.json")
	private Resource unsubscribeConfirmation;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void subscriptionConfirmation() throws Exception {
		this.mockMvc
				.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "SubscriptionConfirmation")
						.contentType(MediaType.APPLICATION_JSON)
						.content(StreamUtils.copyToByteArray(this.subscriptionConfirmation.getInputStream())))
				.andExpect(status().isNoContent());

		Message<?> receive = this.inputChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(SnsHeaders.SNS_MESSAGE_TYPE_HEADER, "SubscriptionConfirmation")
				.containsKey(SnsHeaders.NOTIFICATION_STATUS_HEADER);

		NotificationStatus notificationStatus = (NotificationStatus) receive.getHeaders()
				.get(SnsHeaders.NOTIFICATION_STATUS_HEADER);

		notificationStatus.confirmSubscription();

		verify(this.amazonSns).confirmSubscription(ConfirmSubscriptionRequest.builder()
				.topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
				.token("111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111")
				.build());
	}

	@Test
	@SuppressWarnings("unchecked")
	void notification() throws Exception {
		this.mockMvc
				.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "Notification")
						.contentType(MediaType.TEXT_PLAIN)
						.content(StreamUtils.copyToByteArray(this.notificationMessage.getInputStream())))
				.andExpect(status().isNoContent());

		Message<?> receive = this.inputChannel.receive(10000);
		assertThat(receive).isNotNull();
		Map<String, String> payload = (Map<String, String>) receive.getPayload();

		assertThat(payload).containsEntry("Subject", "asdasd").containsEntry("Message", "asdasd");
	}

	@Test
	void unsubscribe() throws Exception {
		this.mockMvc
				.perform(post("/mySampleTopic").header("x-amz-sns-message-type", "UnsubscribeConfirmation")
						.contentType(MediaType.TEXT_PLAIN)
						.content(StreamUtils.copyToByteArray(this.unsubscribeConfirmation.getInputStream())))
				.andExpect(status().isNoContent());

		Message<?> receive = this.inputChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(SnsHeaders.SNS_MESSAGE_TYPE_HEADER, "UnsubscribeConfirmation")
				.containsKey(SnsHeaders.NOTIFICATION_STATUS_HEADER);
		NotificationStatus notificationStatus = (NotificationStatus) receive.getHeaders()
				.get(SnsHeaders.NOTIFICATION_STATUS_HEADER);

		notificationStatus.confirmSubscription();

		verify(this.amazonSns).confirmSubscription(ConfirmSubscriptionRequest.builder()
				.topicArn("arn:aws:sns:eu-west-1:111111111111:mySampleTopic")
				.token("2336412f37fb687f5d51e6e241d638b05824e9e2f6713b42abaeb8607743f5ba91d34edd2b9dabe2f1616ed77c0f8801ee79911d34dca3d210c228af87bd5d9597bf0d6093a1464e03af6650e992ecf54605e020f04ad3d47796045c9f24d902e72e811a1ad59852cad453f40bddfb45")
				.build());
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public SnsClient amazonSns() {
			return mock();
		}

		@Bean
		public PollableChannel inputChannel() {
			return new QueueChannel();
		}

		@Bean
		public HttpRequestHandler snsInboundChannelAdapter() {
			SnsInboundChannelAdapter adapter = new SnsInboundChannelAdapter(amazonSns(), "/mySampleTopic");
			adapter.setRequestChannel(inputChannel());
			adapter.setHandleNotificationStatus(true);
			return adapter;
		}

	}

}
