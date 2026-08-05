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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
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

	@Test
	void shouldPassConversionHintToSmartPayloadConverter() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listen", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		RecordingSmartMessageConverter payloadConverter = new RecordingSmartMessageConverter();
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("{}").messageId(UUID.randomUUID().toString()).build();

		converter.toMessagingMessage(message, context);

		assertThat(payloadConverter.targetClass).isEqualTo(GenericWrapper.class);
		assertThat(payloadConverter.conversionHint).isSameAs(conversionHint);
		assertThat(payloadConverter.smartOverloadInvoked).isTrue();
	}

	@Test
	void shouldNotUseInferredConversionHintWhenCustomPayloadTypeMapperTakesPrecedence() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listen", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		RecordingSmartMessageConverter payloadConverter = new RecordingSmartMessageConverter();
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		converter.setPayloadTypeMapper(message -> String.class);
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("{}").messageId(UUID.randomUUID().toString()).build();

		converter.toMessagingMessage(message, context);

		assertThat(payloadConverter.targetClass).isEqualTo(String.class);
		assertThat(payloadConverter.conversionHint).isNull();
		assertThat(payloadConverter.smartOverloadInvoked).isFalse();
	}

	@Test
	void shouldFallBackToRegularPayloadConverterWhenConversionHintIsPresent() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listen", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		RecordingMessageConverter payloadConverter = new RecordingMessageConverter();
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		converter.setPayloadMessageConverter(payloadConverter);
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("{}").messageId(UUID.randomUUID().toString()).build();

		converter.toMessagingMessage(message, context);

		assertThat(payloadConverter.targetClass).isEqualTo(GenericWrapper.class);
	}

	@Test
	void shouldDeserializeGenericWrapperUsingListenerMethodConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listen", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("""
				{"value":{"name":"nested-value"}}
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isInstanceOfSatisfying(GenericWrapper.class,
				wrapper -> assertThat(wrapper.value()).isEqualTo(new NestedPojo("nested-value")));
	}

	@Test
	void shouldDeserializeMessageWithGenericCollectionPayloadUsingListenerMethodConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listenToMessage",
				org.springframework.messaging.Message.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		SqsMessageConversionContext context = createConversionContext(List.class, conversionHint);
		Message message = Message.builder().body("""
				[{"name":"first"},{"name":"second"}]
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isEqualTo(List.of(new NestedPojo("first"), new NestedPojo("second")));
	}

	@Test
	void shouldDeserializeGenericWrapperFromBatchElementConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listenToBatch", List.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0).nested();
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("""
				{"value":{"name":"batch-value"}}
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isInstanceOfSatisfying(GenericWrapper.class,
				wrapper -> assertThat(wrapper.value()).isEqualTo(new NestedPojo("batch-value")));
	}

	@Test
	void shouldDeserializeNestedGenericWrapperUsingListenerMethodConversionHint() throws Exception {
		Method listenerMethod = GenericListener.class.getDeclaredMethod("listenToNestedWrapper", GenericWrapper.class);
		MethodParameter conversionHint = new MethodParameter(listenerMethod, 0);
		SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
		SqsMessageConversionContext context = createConversionContext(GenericWrapper.class, conversionHint);
		Message message = Message.builder().body("""
				{"value":[{"name":"first"},{"name":"second"}]}
				""").messageId(UUID.randomUUID().toString()).build();

		org.springframework.messaging.Message<?> result = converter.toMessagingMessage(message, context);

		assertThat(result.getPayload()).isInstanceOfSatisfying(GenericWrapper.class,
				wrapper -> assertThat(wrapper.value())
						.isEqualTo(List.of(new NestedPojo("first"), new NestedPojo("second"))));
	}

	private SqsMessageConversionContext createConversionContext(Class<?> payloadClass, Object conversionHint) {
		SqsMessageConversionContext context = new SqsMessageConversionContext();
		context.setPayloadClass(payloadClass);
		context.setConversionHint(conversionHint);
		return context;
	}

	static class GenericListener {

		void listen(GenericWrapper<NestedPojo> payload) {
		}

		void listenToMessage(org.springframework.messaging.Message<List<NestedPojo>> message) {
		}

		void listenToBatch(List<GenericWrapper<NestedPojo>> payload) {
		}

		void listenToNestedWrapper(GenericWrapper<List<NestedPojo>> payload) {
		}

	}

	record GenericWrapper<T>(T value)
	{
	}

	record NestedPojo(String name) {
	}

	static class RecordingSmartMessageConverter implements SmartMessageConverter {

		private Class<?> targetClass;

		private Object conversionHint;

		private boolean smartOverloadInvoked;

		@Override
		public Object fromMessage(org.springframework.messaging.Message<?> message, Class<?> targetClass) {
			this.targetClass = targetClass;
			this.conversionHint = null;
			this.smartOverloadInvoked = false;
			return "converted";
		}

		@Override
		public Object fromMessage(org.springframework.messaging.Message<?> message, Class<?> targetClass,
				Object conversionHint) {
			this.targetClass = targetClass;
			this.conversionHint = conversionHint;
			this.smartOverloadInvoked = true;
			return "converted";
		}

		@Override
		public org.springframework.messaging.Message<?> toMessage(Object payload, MessageHeaders headers) {
			return null;
		}

		@Override
		public org.springframework.messaging.Message<?> toMessage(Object payload, MessageHeaders headers,
				Object conversionHint) {
			return null;
		}

	}

	static class RecordingMessageConverter implements MessageConverter {

		private Class<?> targetClass;

		@Override
		public Object fromMessage(org.springframework.messaging.Message<?> message, Class<?> targetClass) {
			this.targetClass = targetClass;
			return "converted";
		}

		@Override
		public org.springframework.messaging.Message<?> toMessage(Object payload, MessageHeaders headers) {
			return null;
		}

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
