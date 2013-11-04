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
import org.elasticspring.messaging.support.converter.ObjectMessageConverter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.converter.SimpleMessageConverter;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageListenerAdapterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testProcessBigIntegerMessage() {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new ObjectMessageConverter(), delegate, "receiveIntegerMessage");
		BigInteger myMessage = new BigInteger("1322465468735489");
		Message<String> stringMessage = new ObjectMessageConverter().toMessage(myMessage, null);
		adapter.onMessage(stringMessage);
		assertTrue(delegate.isMethodCalled());
	}

	@Test
	public void testProcessStringMessage() throws Exception {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new SimpleMessageConverter(), delegate, "receiveStringMessage");
		Message<String> stringMessage = MessageBuilder.withPayload("stringMessage").build();
		adapter.onMessage(stringMessage);
		assertTrue(delegate.isMethodCalled());
	}

	@Test
	public void testProcessExceptionThrownInMethod() throws Exception {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new SimpleMessageConverter(), delegate, "exceptionThrowingMethod");
		Message<String> stringMessage = MessageBuilder.withPayload("stringMessage").build();
		try {
			adapter.onMessage(stringMessage);
		} catch (ListenerExecutionFailedException e) {
			assertTrue(IllegalArgumentException.class.isInstance(e.getCause()));
		}
		assertFalse(delegate.isMethodCalled());
	}

	@Test
	public void testMethodNotFound() throws Exception {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new SimpleMessageConverter(), delegate, "nonAvailableMethod");
		Message<String> stringMessage = MessageBuilder.withPayload("stringMessage").build();
		try {
			adapter.onMessage(stringMessage);
		} catch (ListenerExecutionFailedException e) {
			assertTrue(NoSuchMethodException.class.isInstance(e.getCause()));
		}
		assertFalse(delegate.isMethodCalled());
	}

	@Test
	public void testOverloadedMethod() throws Exception {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new SimpleMessageConverter(), delegate, "overloadedMethod");
		Message<String> stringMessage = MessageBuilder.withPayload("stringMessage").build();
		adapter.onMessage(stringMessage);
		assertTrue(delegate.isMethodCalled());
	}

	@Test
	public void testMessageConverterIsNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("messageConverter must not be null");
		TargetBean delegate = new TargetBean();
		//noinspection ResultOfObjectAllocationIgnored
		new MessageListenerAdapter(null, delegate, "overloadedMethod");
	}

	@Test
	public void testDelegateIsNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("delegate must not be null");
		//noinspection ResultOfObjectAllocationIgnored
		new MessageListenerAdapter(new SimpleMessageConverter(), null, "overloadedMethod");
	}

	@Test
	public void testTargetMethodIsNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("listenerMethod must not be null");
		//noinspection ResultOfObjectAllocationIgnored
		new MessageListenerAdapter(new SimpleMessageConverter(), new TargetBean(), null);
	}

	private static class TargetBean {

		private boolean methodCalled;

		@RuntimeUse
		public void receiveIntegerMessage(BigInteger message) {
			assertEquals(new BigInteger("1322465468735489"), message);
			this.methodCalled = true;
		}

		@RuntimeUse
		public void receiveStringMessage(String message) {
			assertEquals("stringMessage", message);
			this.methodCalled = true;
		}

		@RuntimeUse
		public void exceptionThrowingMethod(String message) {
			throw new IllegalArgumentException("Throwing exception for: " + message);
		}

		public boolean isMethodCalled() {
			return this.methodCalled;
		}

		@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
		@RuntimeUse
		public void overloadedMethod(Object message) {
			throw new IllegalArgumentException("Refined method must be called");
		}

		@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
		@RuntimeUse
		public void overloadedMethod(String message) {
			assertEquals("stringMessage", message);
			this.methodCalled = true;
		}

	}
}