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

package io.awspring.cloud.it.messaging;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.core.env.ResourceIdResolver;
import io.awspring.cloud.it.IntegrationTestConfig;
import io.awspring.cloud.messaging.config.annotation.EnableSqs;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.support.converter.ObjectMessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Alain Sahli
 */
@ContextConfiguration(
		classes = JavaQueueMessagingTemplateIntegrationTest.QueueMessagingTemplateIntegrationTestConfiguration.class)
class JavaQueueMessagingTemplateIntegrationTest extends QueueMessagingTemplateIntegrationTest {

	@Configuration
	@EnableSqs
	@Import(IntegrationTestConfig.class)
	protected static class QueueMessagingTemplateIntegrationTestConfiguration {

		@Bean
		public QueueMessagingTemplate defaultQueueMessagingTemplate(AmazonSQSAsync amazonSqs,
				ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("JsonQueue");

			return queueMessagingTemplate;
		}

		@Bean
		public QueueMessagingTemplate queueMessagingTemplateWithCustomConverter(AmazonSQSAsync amazonSqs,
				ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("StreamQueue");
			queueMessagingTemplate.setMessageConverter(new ObjectMessageConverter());

			return queueMessagingTemplate;
		}

	}

}
