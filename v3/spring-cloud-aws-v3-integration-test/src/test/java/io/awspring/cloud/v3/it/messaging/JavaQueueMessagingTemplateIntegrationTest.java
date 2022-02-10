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
import io.awspring.cloud.v3.it.IntegrationTestConfig;
import io.awspring.cloud.v3.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.v3.messaging.support.converter.ObjectMessageConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * @author Alain Sahli
 */
@ContextConfiguration(
		classes = JavaQueueMessagingTemplateIntegrationTest.QueueMessagingTemplateIntegrationTestConfiguration.class)
class JavaQueueMessagingTemplateIntegrationTest extends QueueMessagingTemplateIntegrationTest {

	@Configuration
	@Import(IntegrationTestConfig.class)
	protected static class QueueMessagingTemplateIntegrationTestConfiguration {

		@Bean
		public QueueMessagingTemplate defaultQueueMessagingTemplate(SqsClient amazonSqs,
																	ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("JsonQueue");

			return queueMessagingTemplate;
		}

		@Bean
		public QueueMessagingTemplate queueMessagingTemplateWithCustomConverter(SqsClient amazonSqs,
				ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("StreamQueue");
			queueMessagingTemplate.setMessageConverter(new ObjectMessageConverter());

			return queueMessagingTemplate;
		}

	}

}
