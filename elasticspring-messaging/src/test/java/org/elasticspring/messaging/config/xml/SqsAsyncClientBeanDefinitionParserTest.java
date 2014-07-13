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

package org.elasticspring.messaging.config.xml;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.elasticspring.messaging.support.SuppressingExecutorServiceAdapter;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SqsAsyncClientBeanDefinitionParserTest {

	@Test
	public void parseInternal_minimalConfiguration_createsBufferedClientWithoutExplicitTaskExecutor() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml",getClass()));

		//Assert
		AmazonSQSBufferedAsyncClient sqsBufferedAsyncClient = beanFactory.getBean("customClient", AmazonSQSBufferedAsyncClient.class);
		AmazonSQSAsyncClient asyncClient = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(sqsBufferedAsyncClient, "realSQS");
		assertNotNull(asyncClient);
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) ReflectionTestUtils.getField(asyncClient, "executorService");
		assertEquals(50,threadPoolExecutor.getCorePoolSize());
	}

	@Test
	public void parseInternal_notBuffered_createsAsyncClientWithoutBufferedDecorator() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-not-buffered.xml",getClass()));

		//Assert
		AmazonSQSAsyncClient asyncClient = beanFactory.getBean("customClient", AmazonSQSAsyncClient.class);
		assertNotNull(asyncClient);
		assertTrue(AmazonSQSAsyncClient.class.isInstance(asyncClient));
	}

	@Test
	public void parseInternal_withCustomTasExecutor_createsBufferedClientWithCustomTaskExecutor() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		//Act
		reader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-custom-task-executor.xml",getClass()));

		//Assert
		AmazonSQSBufferedAsyncClient sqsBufferedAsyncClient = beanFactory.getBean("customClient", AmazonSQSBufferedAsyncClient.class);
		AmazonSQSAsyncClient asyncClient = (AmazonSQSAsyncClient) ReflectionTestUtils.getField(sqsBufferedAsyncClient, "realSQS");
		assertNotNull(asyncClient);
		SuppressingExecutorServiceAdapter executor = (SuppressingExecutorServiceAdapter) ReflectionTestUtils.getField(asyncClient, "executorService");
		assertSame(beanFactory.getBean("myThreadPoolTaskExecutor"), ReflectionTestUtils.getField(executor, "taskExecutor"));
	}

}