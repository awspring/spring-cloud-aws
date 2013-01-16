/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.support.destination.DynamicDestinationResolver;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("SimpleMessageListenerContainerAwsTest-context.xml")
public class SimpleMessageListenerContainerAwsTest {

	@Autowired
	private AmazonSQSAsync amazonSQSClient;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Before
	public void setUp() throws Exception {
		String queueUrl = this.testStackEnvironment.getByLogicalId("LoadTestQueue");
		for (int b = 0; b < 10; b++) {
			List<SendMessageBatchRequestEntry> messages = new ArrayList<SendMessageBatchRequestEntry>();
			for (int i = 0; i < 10; i++) {
				messages.add(new SendMessageBatchRequestEntry(Integer.toString(i), new StringBuilder().append("message_").append(b + i).toString()));
			}
			this.amazonSQSClient.sendMessageBatch(new SendMessageBatchRequest(queueUrl, messages));
		}
	}

	@Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testSimpleListen() throws Exception {
		final CountDownLatch messageReceivedCount = new CountDownLatch(100);
		SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
		simpleMessageListenerContainer.setAmazonSQS(new AmazonSQSBufferedAsyncClient(this.amazonSQSClient));
		simpleMessageListenerContainer.setDestinationName(this.testStackEnvironment.getByLogicalId("LoadTestQueue"));
		simpleMessageListenerContainer.setDestinationResolver(new DynamicDestinationResolver(this.amazonSQSClient));
		ConcurrentTaskScheduler taskScheduler = new ConcurrentTaskScheduler(Executors.newCachedThreadPool(), Executors.newScheduledThreadPool(10));
		simpleMessageListenerContainer.setTaskExecutor(taskScheduler);
		simpleMessageListenerContainer.setMaxNumberOfMessages(10);
		simpleMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message<?> message) {
				Assert.assertNotNull(message);
				messageReceivedCount.countDown();
			}
		});
		simpleMessageListenerContainer.afterPropertiesSet();
		simpleMessageListenerContainer.start();
		simpleMessageListenerContainer.stop();
		messageReceivedCount.countDown();
	}
}
