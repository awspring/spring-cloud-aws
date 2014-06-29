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

package org.elasticspring.messaging.endpoint;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class AbstractNotificationMessageHandlerMethodArgumentResolverTest {

	@Test
	public void resolveArgument_SubScriptionMessage_createsMapWithAllFields() throws Exception {
		//Arrange
		AbstractNotificationMessageHandlerMethodArgumentResolver resolver = new AbstractNotificationMessageHandlerMethodArgumentResolver() {

			@Override
			protected Object doResolverArgumentFromNotificationMessage(HashMap<String, String> content) {
				return content;
			}

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return true;
			}
		};

		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServletWebRequest servletWebRequest = new ServletWebRequest(servletRequest);

		byte[] fileContent = FileCopyUtils.copyToByteArray(new ClassPathResource("subscriptionConfirmation.json", AbstractNotificationMessageHandlerMethodArgumentResolver.class).getInputStream());

		servletRequest.setContent(fileContent);

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

		//Act
		@SuppressWarnings("unchecked") HashMap<String,String> content = (HashMap<String, String>) resolver.resolveArgument(null, null, servletWebRequest, null);

		RequestContextHolder.resetRequestAttributes();

		//Assert
		assertEquals("SubscriptionConfirmation",content.get("Type"));
		assertEquals("e267b24c-5532-472f-889d-c2cdd2143bbc", content.get("MessageId"));
		assertEquals("111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111", content.get("Token"));
		assertEquals("arn:aws:sns:eu-west-1:111111111111:mySampleTopic", content.get("TopicArn"));
		assertEquals("You have chosen to subscribe to the topic arn:aws:sns:eu-west-1:721324560415:mySampleTopic.To confirm the subscription, visit the SubscribeURL included in this message.", content.get("Message"));
		assertEquals("https://sns.eu-west-1.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:eu-west-1:111111111111:mySampleTopic&Token=111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111", content.get("SubscribeURL"));
		assertEquals("2014-06-28T10:22:18.086Z", content.get("Timestamp"));
		assertEquals("1", content.get("SignatureVersion"));
		assertEquals("JLdRUR+uhP4cyVW6bRuUSAkUosFMJyO7g7WCAwEUJoB4y8vQE1uDUWGpbQSEbruVTjPEM8hFsf4/95NftfM0W5IgND1uSnv4P/4AYyL+q0bLOJlquzXrw4w2NX3QShS3y+r/gXzo7p/UP4NOr35MGCEGPqHAEe1Coc5S0eaP3JvKU6xY1tcop6ze2RNHTwzhM43dda2bnjPYogAJzA5uHfmSjs3cMVvPCckj3zdLyvxISp+RgrogdvlNyu9ycND1SxagmbzjkBaqvF/4aiSYFxsEXX4e9zuNuHGmXGWgm1ppYUGLSPPJruCsPUa7Ii1mYvpX7SezuFZlAAXXBk0mHg==", content.get("Signature"))        ;
		assertEquals("https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-e372f8ca30337fdb084e8ac449342c77.pem", content.get("SigningCertURL"));
	}


	@Test
	public void resolveArgument_NotificationMessage_createsMapWithAllFields() throws Exception {
		//Arrange
		AbstractNotificationMessageHandlerMethodArgumentResolver resolver = new AbstractNotificationMessageHandlerMethodArgumentResolver() {

			@Override
			protected Object doResolverArgumentFromNotificationMessage(HashMap<String, String> content) {
				return content;
			}

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return true;
			}
		};

		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServletWebRequest servletWebRequest = new ServletWebRequest(servletRequest);

		byte[] fileContent = FileCopyUtils.copyToByteArray(new ClassPathResource("notificationMessage.json", AbstractNotificationMessageHandlerMethodArgumentResolver.class).getInputStream());

		servletRequest.setContent(fileContent);

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

		//Act
		@SuppressWarnings("unchecked") HashMap<String,String> content = (HashMap<String, String>) resolver.resolveArgument(null, null, servletWebRequest, null);

		RequestContextHolder.resetRequestAttributes();

		//Assert
		assertEquals("Notification",content.get("Type"));
		assertEquals("f2c15fec-c617-5b08-b54d-13c4099fec60", content.get("MessageId"));
		assertEquals("arn:aws:sns:eu-west-1:111111111111:mySampleTopic", content.get("TopicArn"));
		assertEquals("asdasd", content.get("Subject"));
		assertEquals("asdasd", content.get("Message"));
		assertEquals("2014-06-28T14:12:24.418Z", content.get("Timestamp"));
		assertEquals("1", content.get("SignatureVersion"));
		assertEquals("XDvKSAnhxECrAmyIrs0Dsfbp/tnKD1IvoOOYTU28FtbUoxr/CgziuW87yZwTuSNNbHJbdD3BEjHS0vKewm0xBeQ0PToDkgtoORXo5RWnmShDQ2nhkthFhZnNulKtmFtRogjBtCwbz8sPnbOCSk21ruyXNdV2RUbdDalndAW002CWEQmYMxFSN6OXUtMueuT610aX+tqeYP4Z6+8WTWLWjAuVyy7rOI6KHYBcVDhKtskvTOPZ4tiVohtQdQbO2Gjuh1vblRzzwMkfaoFTSWImd4pFXxEsv/fq9aGIlqq9xEryJ0w2huFwI5gxyhvGt0RnTd9YvmAEC+WzdJDOqaDNxg==", content.get("Signature"))        ;
		assertEquals("https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-e372f8ca30337fdb084e8ac449342c77.pem", content.get("SigningCertURL"));
		assertEquals("https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:721324560415:mySampleTopic:9859a6c9-6083-4690-ab02-d1aead3442df",content.get("UnsubscribeURL"));
	}
}