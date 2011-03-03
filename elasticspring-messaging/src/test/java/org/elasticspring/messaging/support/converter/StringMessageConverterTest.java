/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.converter;

import org.elasticspring.messaging.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class StringMessageConverterTest {

	@Test
	public void testToAndFromMessage() throws Exception {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		Message message = stringMessageConverter.toMessage("content");
		assertEquals("content",message.getPayload());

		String content = stringMessageConverter.fromMessage(message);
		assertEquals("content",content);
	}
}
