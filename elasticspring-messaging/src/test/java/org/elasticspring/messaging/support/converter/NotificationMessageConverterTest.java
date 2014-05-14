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

package org.elasticspring.messaging.support.converter;

import org.codehaus.jettison.json.JSONObject;
import org.elasticspring.messaging.support.NotificationMessage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class NotificationMessageConverterTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testWriteMessageNotSupported() throws Exception {
		this.expectedException.expect(UnsupportedOperationException.class);
		new NotificationMessageConverter().toMessage("test", null);
	}

	@Test
	public void testReadMessage() throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello");
		jsonObject.put("Message", "World");
		String payload = jsonObject.toString();
		NotificationMessage notificationMessage =
				new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(payload).build(), null);
		Assert.assertEquals("Hello", notificationMessage.getSubject());
		Assert.assertEquals("World", notificationMessage.getBody());
	}

	@Test
	public void testReadMessageOnlyBody() throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Message", "Hello World");
		String payload = jsonObject.toString();
		NotificationMessage notificationMessage =
				new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(payload).build(), null);
		Assert.assertEquals("Hello World", notificationMessage.getBody());
		Assert.assertNull(notificationMessage.getSubject());
	}

	@Test
	public void testNoTypeSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("does not contain a Type attribute");
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(payload).build(), null);
	}

	@Test
	public void testWrongTypeSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("is not a valid notification");
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Subscription");
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(payload).build(), null);
	}

	@Test
	public void testNoMessageAvailableSupplied() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("does not contain a message");
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hello World!");
		String payload = jsonObject.toString();
		new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(payload).build(), null);
	}

	@Test
	public void testNoValidJson() throws Exception {
		this.expectedException.expect(MessageConversionException.class);
		this.expectedException.expectMessage("Error reading payload");
		String message = "foo";
		new NotificationMessageConverter().fromMessage(MessageBuilder.withPayload(message).build(),null);
	}

}
