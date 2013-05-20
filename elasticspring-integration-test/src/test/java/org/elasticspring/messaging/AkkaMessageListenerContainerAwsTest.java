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

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import org.elasticspring.messaging.listener.ActorBasedMessageListenerContainer;
import org.elasticspring.messaging.listener.MessageListener;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("AkkaMessageListenerContainerAwsTest-context.xml")
public class AkkaMessageListenerContainerAwsTest {

	private static final int BATCH_MESSAGE_SIZE = 10;

	private static final int TOTAL_BATCHES = 10;

	private static final int TOTAL_MESSAGES = BATCH_MESSAGE_SIZE * TOTAL_BATCHES;

	@Autowired
	private AmazonSQS amazonSqsClient;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Before
	public void setUp() throws InterruptedException {
		final CountDownLatch countDownLatch = new CountDownLatch(TOTAL_BATCHES);


		ActorSystem actorSystem = ActorSystem.create();
		ActorRef actorRef = actorSystem.actorOf(new Props(new UntypedActorFactory() {

			@Override
			public Actor create() throws Exception {
				return new QueueMessageSender(AkkaMessageListenerContainerAwsTest.this.testStackEnvironment.getByLogicalId("LoadTestQueue"),
						AkkaMessageListenerContainerAwsTest.this.amazonSqsClient, countDownLatch);
			}
		}));

		for (int batch = 0; batch < TOTAL_BATCHES; batch++) {
			actorRef.tell(batch,actorRef);
		}

		countDownLatch.await();
		actorSystem.shutdown();
	}

	@Test
	public void testSimpleListen() throws Exception {
		final CountDownLatch messageReceivedCount = new CountDownLatch(TOTAL_MESSAGES);
		ActorBasedMessageListenerContainer container = new ActorBasedMessageListenerContainer();
		container.setAmazonSqs(this.amazonSqsClient);
		container.setDestinationName(this.testStackEnvironment.getByLogicalId("LoadTestQueue"));
		container.setDestinationResolver(new DynamicQueueDestinationResolver(this.amazonSqsClient));
		container.setMaxNumberOfMessages(10);
		container.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message<String> message) {
				Assert.assertNotNull(message);
				messageReceivedCount.countDown();
			}
		});
		container.setBeanName("test");
		container.afterPropertiesSet();
		container.start();
		messageReceivedCount.await();
		container.stop();
		container.destroy();
	}


	private static class QueueMessageSender extends UntypedActor {

		private final String queueUrl;
		private final AmazonSQS amazonSqs;
		private final CountDownLatch countDownLatch;

		private QueueMessageSender(String queueUrl, AmazonSQS amazonSqs, CountDownLatch countDownLatch) {
			this.queueUrl = queueUrl;
			this.amazonSqs = amazonSqs;
			this.countDownLatch = countDownLatch;
		}

		@Override
		public void onReceive(Object message) throws Exception {
			List<SendMessageBatchRequestEntry> messages = new ArrayList<SendMessageBatchRequestEntry>();
			for (int i = 0; i < 10; i++) {
				messages.add(new SendMessageBatchRequestEntry(Integer.toString(i), new StringBuilder().append("message_").append(message).append(i).toString()));
			}
			this.amazonSqs.sendMessageBatch(new SendMessageBatchRequest(this.queueUrl, messages));
			this.countDownLatch.countDown();
		}
	}
}