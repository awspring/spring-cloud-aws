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

package org.elasticspring.messaging.annotation;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

import java.util.concurrent.CountDownLatch;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueListenerBeanPostProcessorTest {

	@Test
	public void testBeanThatContainsQueueListener() throws Exception {

		GenericApplicationContext applicationContext = new GenericApplicationContext();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonSQSAsync.class);
		applicationContext.registerBeanDefinition("amazonSQS", beanDefinitionBuilder.getBeanDefinition());

		CountDownLatch countDownLatch = new CountDownLatch(1);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(QueueListenerBean.class);
		builder.addConstructorArgValue(countDownLatch);

		String message = "test";
		builder.addConstructorArgValue(message);
		applicationContext.registerBeanDefinition("listenerBean", builder.getBeanDefinition());
		applicationContext.registerBeanDefinition("beanPostProcessor", BeanDefinitionBuilder.rootBeanDefinition(QueueListenerBeanPostProcessor.class).getBeanDefinition());

		AmazonSQSAsync amazonSQS = applicationContext.getBean("amazonSQS", AmazonSQSAsync.class);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://foo/bar"));

		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://foo/bar"))).thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody(message)), new ReceiveMessageResult());

		applicationContext.refresh();

		countDownLatch.await();

		applicationContext.close();
	}

	static class QueueListenerBean {

		private final CountDownLatch countDownLatch;
		private final String expectedContent;

		QueueListenerBean(CountDownLatch countDownLatch, String expectedContent) {
			this.countDownLatch = countDownLatch;
			this.expectedContent = expectedContent;
		}

		@QueueListener("test")
		public void foo(String bar) {
			this.countDownLatch.countDown();
			Assert.assertEquals(this.expectedContent, bar);
		}
	}
}
