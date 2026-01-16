/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for {@link SqsMessagingMessageConverter}.
 *
 * @author Tomaz Fernandes
 */
class SqsMessagingMessageConverterTests {

	@Test
	void shouldCreateConverterWithDefaultJsonMapper() {
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();

		assertThat(converter).extracting("payloadMessageConverter").asInstanceOf(type(CompositeMessageConverter.class))
				.extracting(CompositeMessageConverter::getConverters).asList()
				.filteredOn(c -> c instanceof JacksonJsonMessageConverter).hasSize(1).first().extracting("mapper")
				.isNotNull();
	}

	@Test
	void shouldCreateConverterWithProvidedJsonMapper() {
		JsonMapper customMapper = JsonMapper.builder().build();

		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter(customMapper);

		assertThat(converter).extracting("payloadMessageConverter").asInstanceOf(type(CompositeMessageConverter.class))
				.extracting(CompositeMessageConverter::getConverters).asList()
				.filteredOn(c -> c instanceof JacksonJsonMessageConverter).hasSize(1).first().extracting("mapper")
				.isSameAs(customMapper);
	}

	@Test
	void shouldThrowWhenJsonMapperIsNull() {
		assertThatThrownBy(() -> new SqsMessagingMessageConverter(null)).isInstanceOf(NullPointerException.class)
				.hasMessageContaining("jsonMapper cannot be null");
	}

	@Test
	void shouldConvertMessageWithCustomJsonMapper() throws Exception {
		JsonMapper customMapper = JsonMapper.builder().build();
		MyPojo myPojo = new MyPojo();
		String payload = customMapper.writeValueAsString(myPojo);
		Message message = Message.builder().body(payload).messageId(UUID.randomUUID().toString()).build();

		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter(customMapper);
		converter.setPayloadTypeMapper(msg -> MyPojo.class);

		org.springframework.messaging.Message<?> resultMessage = converter.toMessagingMessage(message);

		assertThat(resultMessage.getPayload()).isEqualTo(myPojo);
	}

	static class MyPojo {

		private String myProperty = "myValue";

		public String getMyProperty() {
			return this.myProperty;
		}

		public void setMyProperty(String myProperty) {
			this.myProperty = myProperty;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			MyPojo myPojo = (MyPojo) o;
			return Objects.equals(myProperty, myPojo.myProperty);
		}

		@Override
		public int hashCode() {
			return myProperty != null ? myProperty.hashCode() : 0;
		}
	}

}