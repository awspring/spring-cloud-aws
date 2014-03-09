/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.messaging;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("MessageListenerContainerAwsTest-context.xml")
public class MessageListenerContainerAwsTest {

	private static final int BATCH_MESSAGE_SIZE = 10;

	private static final int TOTAL_BATCHES = 100;

	private static final int TOTAL_MESSAGES = BATCH_MESSAGE_SIZE * TOTAL_BATCHES;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Autowired
	private AmazonSQS amazonSqsClient;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private MessageReceiver messageReceiver;

	@Before
	public void setUp() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(TOTAL_BATCHES);

		for (int batch = 0; batch < TOTAL_BATCHES; batch++) {
			this.taskExecutor.execute(new QueueMessageSender(this.testStackEnvironment.getByLogicalId("LoadTestQueue"), this.amazonSqsClient, countDownLatch));
		}

		countDownLatch.await();
	}

	@Test
	public void testSimpleListen() throws Exception {
		this.messageReceiver.getCountDownLatch().await();
	}

	static class MessageReceiver {

		private final CountDownLatch countDownLatch = new CountDownLatch(TOTAL_MESSAGES);

		@RuntimeUse
		@MessageMapping("LoadTestQueue")
		public void onMessage(String message) {
			Assert.assertNotNull(message);
			this.getCountDownLatch().countDown();
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}
	}


	private static class QueueMessageSender implements Runnable {

		private final String queueUrl;
		private final AmazonSQS amazonSqs;
		private final CountDownLatch countDownLatch;

		private QueueMessageSender(String queueUrl, AmazonSQS amazonSqs, CountDownLatch countDownLatch) {
			this.queueUrl = queueUrl;
			this.amazonSqs = amazonSqs;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void run() {
			List<SendMessageBatchRequestEntry> messages = new ArrayList<SendMessageBatchRequestEntry>();
			for (int i = 0; i < 10; i++) {
				messages.add(new SendMessageBatchRequestEntry(Integer.toString(i), new StringBuilder().append("message_").append(i).toString()));
			}
			this.amazonSqs.sendMessageBatch(new SendMessageBatchRequest(this.queueUrl, messages));
			this.countDownLatch.countDown();
		}
	}
}