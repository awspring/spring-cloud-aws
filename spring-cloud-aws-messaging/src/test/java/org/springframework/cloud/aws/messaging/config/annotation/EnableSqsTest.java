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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.services.sqs.AmazonSQS;
import org.junit.Test;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
public class EnableSqsTest {

	@Test
	public void simpleMessageListenerContainer_withCredentialsAvailable_shouldBeUpAndRunning() throws Exception {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(EnableSqsTestConfiguration.class);
		SimpleMessageListenerContainer simpleMessageListenerContainer = applicationContext.getBean(SimpleMessageListenerContainer.class);
		assertTrue(simpleMessageListenerContainer.isRunning());
		assertTrue(QueueMessageHandler.class.isInstance(ReflectionTestUtils.getField(simpleMessageListenerContainer, "messageHandler")));
	}

	@Configuration
	@EnableSqs
	public static class EnableSqsTestConfiguration {

		@Bean
		public AmazonSQS amazonSqs() {
			return mock(AmazonSQS.class);
		}

	}

}