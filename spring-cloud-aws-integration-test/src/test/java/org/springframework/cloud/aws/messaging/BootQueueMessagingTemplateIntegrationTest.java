/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.support.converter.ObjectMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Alain Sahli
 */
@SpringApplicationConfiguration(classes = BootQueueMessagingTemplateIntegrationTest.QueueMessagingTemplateIntegrationTestConfiguration.class)
@IntegrationTest
public class BootQueueMessagingTemplateIntegrationTest extends QueueMessagingTemplateIntegrationTest {

	@Configuration
	@EnableAutoConfiguration
	@PropertySource({"classpath:Integration-test-config.properties", "file://${els.config.dir}/access.properties"})
	protected static class QueueMessagingTemplateIntegrationTestConfiguration {

		@Bean
		public QueueMessagingTemplate defaultQueueMessagingTemplate(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("JsonQueue");

			return queueMessagingTemplate;
		}

		@Bean
		public QueueMessagingTemplate queueMessagingTemplateWithCustomConverter(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
			QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
			queueMessagingTemplate.setDefaultDestinationName("StreamQueue");
			queueMessagingTemplate.setMessageConverter(new ObjectMessageConverter());

			return queueMessagingTemplate;
		}

	}

}
