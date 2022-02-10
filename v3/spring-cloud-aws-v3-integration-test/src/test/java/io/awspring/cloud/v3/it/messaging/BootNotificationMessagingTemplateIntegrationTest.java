/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.it.messaging;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import io.awspring.cloud.v3.messaging.core.NotificationMessagingTemplate;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * @author Alain Sahli
 */
@SpringBootTest(
		classes = BootNotificationMessagingTemplateIntegrationTest.NotificationMessagingTemplateIntegrationTestConfiguration.class)
class BootNotificationMessagingTemplateIntegrationTest extends NotificationMessagingTemplateIntegrationTest {

	@Configuration
	@EnableAutoConfiguration
	protected static class NotificationMessagingTemplateIntegrationTestConfiguration {

		@Bean
		public NotificationMessagingTemplate notificationMessagingTemplate(SnsClient amazonSns,
																		   ResourceIdResolver resourceIdResolver) {
			NotificationMessagingTemplate notificationMessagingTemplate = new NotificationMessagingTemplate(amazonSns,
					resourceIdResolver);
			notificationMessagingTemplate.setDefaultDestinationName("SqsReceivingSnsTopic");
			return notificationMessagingTemplate;
		}

		@Bean
		public NotificationReceiver notificationReceiver() {
			return new NotificationReceiver();
		}

	}

}
