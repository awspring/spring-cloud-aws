/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.config.xml;

import java.util.concurrent.ThreadPoolExecutor;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.core.task.ShutdownSuppressingExecutorServiceAdapter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SqsAsyncClientBeanDefinitionParserTest {

	@Test
	public void parseInternal_minimalConfiguration_createsBufferedClientWithoutExplicitTaskExecutor()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient sqsBufferedAsyncClient = beanFactory
				.getBean("customClient", AmazonSQSBufferedAsyncClient.class);
		AmazonSQSAsyncClient asyncClient = (AmazonSQSAsyncClient) ReflectionTestUtils
				.getField(sqsBufferedAsyncClient, "realSQS");
		assertThat(asyncClient).isNotNull();
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) ReflectionTestUtils
				.getField(asyncClient, "executorService");
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(50);
	}

	@Test
	public void parseInternal_notBuffered_createsAsyncClientWithoutBufferedDecorator()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-not-buffered.xml", getClass()));

		// Assert
		AmazonSQSAsyncClient asyncClient = beanFactory.getBean("customClient",
				AmazonSQSAsyncClient.class);
		assertThat(asyncClient).isNotNull();
		assertThat(AmazonSQSAsyncClient.class.isInstance(asyncClient)).isTrue();
	}

	@Test
	public void parseInternal_withCustomTasExecutor_createsBufferedClientWithCustomTaskExecutor()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient sqsBufferedAsyncClient = beanFactory
				.getBean("customClient", AmazonSQSBufferedAsyncClient.class);
		AmazonSQSAsyncClient asyncClient = (AmazonSQSAsyncClient) ReflectionTestUtils
				.getField(sqsBufferedAsyncClient, "realSQS");
		assertThat(asyncClient).isNotNull();
		ShutdownSuppressingExecutorServiceAdapter executor = (ShutdownSuppressingExecutorServiceAdapter) ReflectionTestUtils
				.getField(asyncClient, "executorService");
		assertThat(ReflectionTestUtils.getField(executor, "taskExecutor"))
				.isSameAs(beanFactory.getBean("myThreadPoolTaskExecutor"));
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSqs = registry
				.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils.getField(amazonSqs, "realSQS");
		assertThat(
				ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.EU_WEST_1)
								.getServiceEndpoint("sqs"));
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonSQSBufferedAsyncClient amazonSqs = registry
				.getBean(AmazonSQSBufferedAsyncClient.class);
		Object amazonSqsAsyncClient = ReflectionTestUtils.getField(amazonSqs, "realSQS");
		assertThat(
				ReflectionTestUtils.getField(amazonSqsAsyncClient, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2)
								.getServiceEndpoint("sqs"));
	}

}
