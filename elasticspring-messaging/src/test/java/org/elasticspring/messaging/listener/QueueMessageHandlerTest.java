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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueMessageHandlerTest {

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
	public void receiveAndReplyMessage_methodAnnotatedWithMessageMappingAnnotation_methodInvokedForIncomingMessageAndReplySentBackToSendToDestination() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receiveAndReply").build());

		IncomingMessageHandler messageListener = applicationContext.getBean(IncomingMessageHandler.class);
		Assert.assertEquals("TESTCONTENT", messageListener.getLastReceivedMessage());
	}

	@SuppressWarnings("UnusedDeclaration")
	private static class IncomingMessageHandler {

		private String lastReceivedMessage;

		@MessageMapping("receive")
		public void receive(@Payload String value) {
			this.lastReceivedMessage = value;
		}

		@MessageMapping("receiveAndReply")
		@SendTo("receive")
		public String receiveAndReply(String value) {
			return value.toUpperCase();
		}

		private String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}
	}
}
