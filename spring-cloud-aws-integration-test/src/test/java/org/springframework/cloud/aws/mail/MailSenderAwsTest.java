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

package org.springframework.cloud.aws.mail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test that uses the Amazon Simple Mail service to send mail.
 * <p>
 * <br>
 * Note:</br>
 * This test is a fire and forget test as the amazon simple mail service does not provide
 * timely feedback if a message is send or not. Using the
 * {@link com.amazonaws.services.simpleemail.AmazonSimpleEmailService} method to get the
 * send statistics, does not help as the statistics there are only updated after a couple
 * of minutes. Using an IMAP/POP3 account is to complicated for the test to be implemented
 * in terms of mailbox setup etc. The main purpose of this test is to ensure that the api
 * is correctly implemented and the webservice acknowledges the message.
 * </p>
 *
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class MailSenderAwsTest {

	@Autowired
	private MailSender mailSender;

	@Autowired
	private JavaMailSender javaMailSender;

	@Value("#{mail.senderAddress}")
	private String senderAddress;

	@Value("#{mail.recipientAddress}")
	private String recipientAddress;

	@Test
	public void send_sendMailWithoutAnyAttachmentUsingTheSimpleMailApi_noExceptionThrownDuringSendAndForget()
			throws Exception {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom(this.senderAddress);
		simpleMailMessage.setTo(this.recipientAddress);
		simpleMailMessage.setSubject("test subject");
		simpleMailMessage.setText("test content");

		this.mailSender.send(simpleMailMessage);
	}

	@Test
	public void send_sendMailWithAttachmentUsingTheJavaMailMimeMessageFormat_noExceptionThrownDuringMessaegConstructionAndSend()
			throws Exception {
		this.javaMailSender.send(mimeMessage -> {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.addTo(this.recipientAddress);
			helper.setFrom(this.senderAddress);
			helper.addAttachment("test.txt",
					new ByteArrayResource("attachment content".getBytes("UTF-8")));
			helper.setSubject("test subject with attachment");
			helper.setText("mime body", false);
		});
	}

}
