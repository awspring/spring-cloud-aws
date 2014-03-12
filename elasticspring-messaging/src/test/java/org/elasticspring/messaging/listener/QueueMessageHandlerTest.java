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

package org.elasticspring.messaging.listener;

import org.elasticspring.core.support.documentation.RuntimeUse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class QueueMessageHandlerTest {

	@Mock
	private MessageSendingOperations<String> messageTemplate;

	@Test
	public void receiveMessage_methodAnnotatedWithMessageMappingAnnotation_methodInvokedForIncomingMessage() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receive").build());

		IncomingMessageHandler messageListener = applicationContext.getBean(IncomingMessageHandler.class);
		Assert.assertEquals("testContent", messageListener.getLastReceivedMessage());
	}

	@Test
	public void receiveMessage_methodWithCustomObjectAsParameter_parameterIsConverted() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandlerWithCustomParameter.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		DummyKeyValueHolder messagePayload = new DummyKeyValueHolder("myKey", "A value");
		messageHandler.handleMessage(MessageBuilder.withPayload(messagePayload).setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue").build());

		IncomingMessageHandlerWithCustomParameter messageListener = applicationContext.getBean(IncomingMessageHandlerWithCustomParameter.class);
		Assert.assertNotNull(messageListener.getLastReceivedMessage());
		Assert.assertEquals("myKey", messageListener.getLastReceivedMessage().getKey());
		Assert.assertEquals("A value", messageListener.getLastReceivedMessage().getValue());
	}

	@Test
	public void receiveAndReplyMessage_methodAnnotatedWithMessageMappingAnnotation_methodInvokedForIncomingMessageAndReplySentBackToSendToDestination() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);
		applicationContext.registerBeanDefinition("queueMessageHandler", getQueueMessageHandlerBeanDefinition());
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receiveAndReply").build());

		IncomingMessageHandler messageListener = applicationContext.getBean(IncomingMessageHandler.class);
		Assert.assertEquals("testContent", messageListener.getLastReceivedMessage());
		Mockito.verify(this.messageTemplate).convertAndSend(Mockito.eq("sendTo"), Mockito.eq("TESTCONTENT"));
	}

	private AbstractBeanDefinition getQueueMessageHandlerBeanDefinition() {
		BeanDefinitionBuilder queueMessageHandlerBeanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueMessageHandler.class);
		queueMessageHandlerBeanDefinitionBuilder.addPropertyValue("sendToMessageTemplate", this.messageTemplate);
		return queueMessageHandlerBeanDefinitionBuilder.getBeanDefinition();
	}

	@Test
	public void receiveMessage_methodAnnotatedWithMessageMappingContainingMultipleQueueNames_methodInvokedForEachQueueName() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandlerWithMultipleQueueNames", IncomingMessageHandlerWithMultipleQueueNames.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		IncomingMessageHandlerWithMultipleQueueNames incomingMessageHandler = applicationContext.getBean(IncomingMessageHandlerWithMultipleQueueNames.class);

		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from queue one!").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "queueOne").build());
		Assert.assertEquals("Hello from queue one!", incomingMessageHandler.getLastReceivedMessage());

		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from queue two!").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "queueTwo").build());
		Assert.assertEquals("Hello from queue two!", incomingMessageHandler.getLastReceivedMessage());
	}

	@SuppressWarnings("UnusedDeclaration")
	private static class IncomingMessageHandler {

		private String lastReceivedMessage;

		@MessageMapping("receive")
		public void receive(@Payload String value) {
			this.lastReceivedMessage = value;
		}

		@MessageMapping("receiveAndReply")
		@SendTo("sendTo")
		public String receiveAndReply(String value) {
			this.lastReceivedMessage = value;
			return value.toUpperCase();
		}

		private String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}
	}

	private static class IncomingMessageHandlerWithMultipleQueueNames {

		private String lastReceivedMessage;

		public String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@MessageMapping({"queueOne", "queueTwo"})
		public void receive(String value) {
			this.lastReceivedMessage = value;
		}
	}

	private static class DummyKeyValueHolder {

		private final String key;
		private final String value;

		private DummyKeyValueHolder(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return this.key;
		}

		public String getValue() {
			return this.value;
		}
	}

	private static class IncomingMessageHandlerWithCustomParameter {

		private DummyKeyValueHolder lastReceivedMessage;

		public DummyKeyValueHolder getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@MessageMapping("testQueue")
		public void receive(DummyKeyValueHolder value) {
			this.lastReceivedMessage = value;
		}
	}
}
