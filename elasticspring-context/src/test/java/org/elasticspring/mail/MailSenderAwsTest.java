/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.mail;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MailSenderAwsTest {

	@Autowired
	private MailSender mailSender;

	@Value("#{mail.senderAddress}")
	private String senderAddress;

	@Value("#{mail.recipientAddress}")
	private String recipientAddress;

	@org.junit.Test
	@IfProfileValue(name = "test-groups", value = "aws-test")
	public void testSendMail() throws Exception {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom(this.senderAddress);
		simpleMailMessage.setTo(this.recipientAddress);
		simpleMailMessage.setSubject("test subject");
		simpleMailMessage.setText("test content");

		this.mailSender.send(simpleMailMessage);
	}
}
