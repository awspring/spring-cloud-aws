/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.mail.config.xml;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.net.URI;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.makeAccessible;

/**
 * @author Agim Emruli
 */
public class SimpleEmailServiceBeanDefinitionParserTest {


	@Test
	public void parse_MailSenderWithMinimalConfiguration_createMailSenderWithJavaMail() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		//Act
		AmazonSimpleEmailServiceClient emailService = context.getBean(getBeanName(AmazonSimpleEmailServiceClient.class.getName()), AmazonSimpleEmailServiceClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		//Assert
		assertEquals("https://email.us-west-2.amazonaws.com", getEndpointUrlFromWebserviceClient(emailService));

		assertTrue(mailSender instanceof JavaMailSender);
	}

	@Test
	public void parse_MailSenderWithRegionConfiguration_createMailSenderWithJavaMailAndRegion() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-region.xml", getClass());

		//Act
		AmazonSimpleEmailServiceClient emailService = context.getBean(getBeanName(AmazonSimpleEmailServiceClient.class.getName()), AmazonSimpleEmailServiceClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		//Assert
		assertEquals("https://email.eu-west-1.amazonaws.com", getEndpointUrlFromWebserviceClient(emailService));

		assertTrue(mailSender instanceof JavaMailSender);
	}

	@Test
	public void parse_MailSenderWithRegionProviderConfiguration_createMailSenderWithJavaMailAndRegionFromRegionProvider() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-regionProvider.xml", getClass());

		//Act
		AmazonSimpleEmailServiceClient emailService = context.getBean(getBeanName(AmazonSimpleEmailServiceClient.class.getName()), AmazonSimpleEmailServiceClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		//Assert
		assertEquals("https://email.ap-southeast-2.amazonaws.com", getEndpointUrlFromWebserviceClient(emailService));

		assertTrue(mailSender instanceof JavaMailSender);
	}

	@Test
	public void parse_MailSenderWithCustomSesClient_createMailSenderWithCustomSesClient() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ses-client.xml", getClass());

		//Act
		AmazonSimpleEmailServiceClient emailService = context.getBean("emailServiceClient", AmazonSimpleEmailServiceClient.class);

		MailSender mailSender = context.getBean(MailSender.class);

		//Assert
		assertSame(emailService, ReflectionTestUtils.getField(mailSender, "emailService"));
	}

	private static String getEndpointUrlFromWebserviceClient(AmazonSimpleEmailServiceClient client) throws Exception {
		Field field = findField(AmazonSimpleEmailServiceClient.class, "endpoint");
		makeAccessible(field);
		URI endpointUri = (URI) field.get(client);
		return endpointUri.toASCIIString();
	}
}
