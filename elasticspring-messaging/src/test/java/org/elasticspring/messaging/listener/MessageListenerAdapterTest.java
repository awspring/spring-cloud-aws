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

import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.support.converter.ObjectMessageConverter;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageListenerAdapterTest {

	@Test
	public void testMessageListenerAdapter() {
		TargetBean delegate = new TargetBean();
		MessageListenerAdapter adapter = new MessageListenerAdapter(new ObjectMessageConverter(), delegate, "receiveMessage");
		BigInteger myMessage = new BigInteger("1322465468735489");
		Message<String> stringMessage = new ObjectMessageConverter().toMessage(myMessage);
		adapter.onMessage(stringMessage);
		assertTrue(delegate.isMethodCalled());
	}

	private static class TargetBean {

		private boolean methodCalled = false;

		public void receiveMessage(BigInteger message) {
			assertEquals(message, new BigInteger("1322465468735489"));
			this.methodCalled = true;
		}

		public boolean isMethodCalled() {
			return this.methodCalled;
		}
	}

}
