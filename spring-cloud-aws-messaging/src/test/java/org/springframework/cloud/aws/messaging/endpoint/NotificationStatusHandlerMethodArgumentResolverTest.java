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

package org.springframework.cloud.aws.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NotificationStatusHandlerMethodArgumentResolverTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void resolveArgument_wrongMessageType_reportsErrors() throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("NotificationStatus is only available");

		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusHandlerMethodArgumentResolver resolver = new NotificationStatusHandlerMethodArgumentResolver(
				amazonSns);

		byte[] subscriptionRequestJsonContent = FileCopyUtils.copyToByteArray(
				new ClassPathResource("notificationMessage.json", getClass())
						.getInputStream());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class,
						"subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		resolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(servletRequest), null);

		// Assert
	}

	@Test
	public void resolveArgument_subscriptionRequest_createsValidSubscriptionStatus()
			throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);
		NotificationStatusHandlerMethodArgumentResolver resolver = new NotificationStatusHandlerMethodArgumentResolver(
				amazonSns);

		byte[] subscriptionRequestJsonContent = FileCopyUtils.copyToByteArray(
				new ClassPathResource("subscriptionConfirmation.json", getClass())
						.getInputStream());

		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setContent(subscriptionRequestJsonContent);

		MethodParameter methodParameter = new MethodParameter(
				ReflectionUtils.findMethod(NotificationMethods.class,
						"subscriptionMethod", NotificationStatus.class),
				0);

		// Act
		Object resolvedArgument = resolver.resolveArgument(methodParameter, null,
				new ServletWebRequest(servletRequest), null);

		// Assert
		assertThat(resolvedArgument instanceof NotificationStatus).isTrue();
		((NotificationStatus) resolvedArgument).confirmSubscription();
		verify(amazonSns, times(1)).confirmSubscription(
				"arn:aws:sns:eu-west-1:111111111111:mySampleTopic",
				"1111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111"
						+ "11111111111111111111111111111111111");
	}

}
