/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.sns.handlers.webflux;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sns.handlers.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @author Matej Nedic
 */
class WebFluxBaseNotificationMessageHandlerMethodArgumentResolverTest {

	@Test
	void resolveArgument_SubscriptionMessage_createsObjectWithAllFields() throws Exception {
		// Arrange
		WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver() {

			@Override
			protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content,
					ServerWebExchange exchange, Class<?> parameterType) {
				return Mono.just(content);
			}

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return true;
			}
		};

		byte[] fileContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(fileContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Act
		ObjectNode content = (ObjectNode) resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(content.get("Type").asString()).isEqualTo("SubscriptionConfirmation");
		assertThat(content.get("MessageId").asString()).isEqualTo("e267b24c-5532-472f-889d-c2cdd2143bbc");
		assertThat(content.get("Token").asString())
				.isEqualTo("111111111111111111111111111111111111111111111111111111111"
						+ "111111111111111111111111111111111111111111111111111111111111111"
						+ "111111111111111111111111111111111111111111111111111111111111111"
						+ "111111111111111111111111111");
		assertThat(content.get("TopicArn").asString()).isEqualTo("arn:aws:sns:eu-west-1:111111111111:mySampleTopic");
		assertThat(content.get("Message").asString())
				.isEqualTo("You have chosen to subscribe to the topic arn:aws:sns:eu-west-1:721324560415:mySampleTopic."
						+ "To confirm the subscription, visit the SubscribeURL included in this message.");
		assertThat(content.get("SubscribeURL").asString())
				.isEqualTo("https://sns.eu-west-1.amazonaws.com/?Action=ConfirmSubscription&"
						+ "TopicArn=arn:aws:sns:eu-west-1:111111111111:mySampleTopic"
						+ "&Token=11111111111111111111111111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111111111111111111111111"
						+ "1111111111111111111111111111111111111111111111111111111111111111" + "11111111111111");
		assertThat(content.get("Timestamp").asString()).isEqualTo("2014-06-28T10:22:18.086Z");
		assertThat(content.get("SignatureVersion").asString()).isEqualTo("1");
		assertThat(content.get("Signature").asString()).isEqualTo("JLdRUR+uhP4cyVW6bRuUSAkUosFMJyO7g7WCAwEUJoB4"
				+ "y8vQE1uDUWGpbQSEbruVTjPEM8hFsf4/95NftfM0W5IgND1uS" + "nv4P/4AYyL+q0bLOJlquzXrw4w2NX3QShS3y+r/gXzo7p"
				+ "/UP4NOr35MGCEGPqHAEe1Coc5S0eaP3JvKU6xY1tcop6ze2RNH"
				+ "TwzhM43dda2bnjPYogAJzA5uHfmSjs3cMVvPCckj3zdLyvxISp"
				+ "+RgrogdvlNyu9ycND1SxagmbzjkBaqvF/4aiSYFxsEXX4e9zuNu"
				+ "HGmXGWgm1ppYUGLSPPJruCsPUa7Ii1mYvpX7SezuFZlAAXXBk0mHg==");
		assertThat(content.get("SigningCertURL").asString()).isEqualTo(
				"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-e372f8ca30337fdb084e8ac449342c77.pem");
	}

	@Test
	void resolveArgument_calledTwiceOnSameExchange_reusesCachedBody() throws Exception {
		// Arrange
		WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver() {

			@Override
			protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content,
					ServerWebExchange exchange, Class<?> parameterType) {
				return Mono.just(content);
			}

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return true;
			}
		};

		byte[] fileContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("subscriptionConfirmation.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(fileContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Act
		ObjectNode first = (ObjectNode) resolver.resolveArgument(methodParameter, null, exchange).block();
		ObjectNode second = (ObjectNode) resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(first).isSameAs(second);
	}

	@Test
	void resolveArgument_NotificationMessage_createsObjectWithAllFields() throws Exception {
		// Arrange
		WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver resolver = new WebFluxAbstractNotificationMessageHandlerMethodArgumentResolver() {

			@Override
			protected Mono<Object> doResolveArgumentFromNotificationMessage(JsonNode content,
					ServerWebExchange exchange, Class<?> parameterType) {
				return Mono.just(content);
			}

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return true;
			}
		};

		byte[] fileContent = FileCopyUtils
				.copyToByteArray(getClass().getClassLoader().getResourceAsStream("notificationMessage.json"));

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON)
				.body(new String(fileContent));
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		MethodParameter methodParameter = new MethodParameter(ReflectionUtils
				.findMethod(WebFluxNotificationMethods.class, "subscriptionMethod", NotificationStatus.class), 0);

		// Act
		ObjectNode content = (ObjectNode) resolver.resolveArgument(methodParameter, null, exchange).block();

		// Assert
		assertThat(content.get("Type").asString()).isEqualTo("Notification");
		assertThat(content.get("MessageId").asString()).isEqualTo("f2c15fec-c617-5b08-b54d-13c4099fec60");
		assertThat(content.get("TopicArn").asString()).isEqualTo("arn:aws:sns:eu-west-1:111111111111:mySampleTopic");
		assertThat(content.get("Subject").asString()).isEqualTo("asdasd");
		assertThat(content.get("Message").asString()).isEqualTo("asdasd");
		assertThat(content.get("Timestamp").asString()).isEqualTo("2014-06-28T14:12:24.418Z");
		assertThat(content.get("SignatureVersion").asString()).isEqualTo("1");
		assertThat(content.get("Signature").asString()).isEqualTo("XDvKSAnhxECrAmyIrs0Dsfbp/tnKD1IvoOOYTU28FtbUoxr"
				+ "/CgziuW87yZwTuSNNbHJbdD3BEjHS0vKewm0xBeQ0PToDkgtoORXo"
				+ "5RWnmShDQ2nhkthFhZnNulKtmFtRogjBtCwbz8sPnbOCSk21ruyXNd"
				+ "V2RUbdDalndAW002CWEQmYMxFSN6OXUtMueuT610aX+tqeYP4Z6+8WT"
				+ "WLWjAuVyy7rOI6KHYBcVDhKtskvTOPZ4tiVohtQdQbO2Gjuh1vbl"
				+ "RzzwMkfaoFTSWImd4pFXxEsv/fq9aGIlqq9xEryJ0w2huFwI5gxyhvGt0RnTd9YvmAEC+WzdJDOqaDNxg==");
		assertThat(content.get("SigningCertURL").asString()).isEqualTo(
				"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-e372f8ca30337fdb084e8ac449342c77.pem");
		assertThat(content.get("UnsubscribeURL").asString())
				.isEqualTo("https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn="
						+ "arn:aws:sns:eu-west-1:721324560415:mySampleTopic:9859a6c9-6083-4690-ab02-d1aead3442df");
	}

}
