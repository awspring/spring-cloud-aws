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

package org.elasticspring.messaging;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.List;


/**
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SendMessageTest {


	@Autowired
	@Qualifier("stringMessage")
	private MessageOperations stringMessageOperations;

	@Autowired
	@Qualifier("objectMessage")
	private MessageOperations objectMessageOperations;

    @Test
	@IfProfileValue(name= "test-groups", value = "integration-test")
    public void testSendAndReceiveStringMessage() throws Exception {
		String messageContent = "testMessage";
		stringMessageOperations.convertAndSend(messageContent);

		String receivedMessage = (String) stringMessageOperations.receiveAndConvert();
		Assert.assertEquals(messageContent, receivedMessage);

    }

	@Test
	@IfProfileValue(name= "test-groups", value = "integration-test")
    public void testSendAndReceiveObjectMessage() throws Exception {
		List<String> payload = Collections.singletonList("myString");
		objectMessageOperations.convertAndSend(payload);

		@SuppressWarnings({"unchecked"})
		List<String> result = (List<String>) objectMessageOperations.receiveAndConvert();
		Assert.assertEquals("myString",result.get(0));
    }
}