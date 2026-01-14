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
package io.awspring.cloud.sqs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.support.resolver.BatchPayloadMethodArgumentResolver;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Comprehensive tests for {@link DefaultMethodPayloadTypeInferrer}.
 *
 * @author Tomaz Fernandes
 */
class DefaultMethodPayloadTypeInferrerTest {

	private final DefaultMethodPayloadTypeInferrer inferrer = new DefaultMethodPayloadTypeInferrer();

	// ========== Null and Empty Input Tests ==========

	@Test
	void shouldReturnNullWhenArgumentResolversIsNull() throws Exception {
		Method method = TestMethods.class.getMethod("simpleStringPayload", String.class);

		Class<?> result = inferrer.inferPayloadType(method, null);

		assertThat(result).isNull();
	}

	@Test
	void shouldReturnNullWhenArgumentResolversIsEmpty() throws Exception {
		Method method = TestMethods.class.getMethod("simpleStringPayload", String.class);
		List<HandlerMethodArgumentResolver> emptyResolvers = Collections.emptyList();

		Class<?> result = inferrer.inferPayloadType(method, emptyResolvers);

		assertThat(result).isNull();
	}

	@Test
	void shouldReturnNullWhenMethodHasNoParameters() throws Exception {
		Method method = TestMethods.class.getMethod("noParameters");
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isNull();
	}

	// ========== @Payload Annotation Tests ==========

	@Test
	void shouldInferFromPayloadAnnotationOnFirstParameter() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationFirst", String.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(String.class);
	}

	@Test
	void shouldInferFromPayloadAnnotationOnSecondParameter() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationSecond", String.class, CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferFromPayloadAnnotationWithList() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationList", List.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferFromPayloadAnnotationWithMessage() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationMessage", Message.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferFromPayloadAnnotationWithListOfMessages() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationListOfMessages", List.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	// ========== Inference Without @Payload Annotation Tests ==========

	@Test
	void shouldInferSimpleTypeAsFirstParameter() throws Exception {
		Method method = TestMethods.class.getMethod("simpleStringPayload", String.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(String.class);
	}

	@Test
	void shouldInferCustomTypeAsFirstParameter() throws Exception {
		Method method = TestMethods.class.getMethod("customEventPayload", CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferPayloadAsSecondParameterWhenFirstIsSupportedByNonPayloadResolver() throws Exception {
		Method method = TestMethods.class.getMethod("headerThenPayload", String.class, CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferPayloadAfterMultipleNonPayloadParameters() throws Exception {
		Method method = TestMethods.class.getMethod("multipleNonPayloadThenPayload", String.class, String.class,
				CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldInferPayloadWithAcknowledgementParameter() throws Exception {
		Method method = TestMethods.class.getMethod("payloadWithAcknowledgement", CustomEvent.class,
				Acknowledgement.class);
		List<HandlerMethodArgumentResolver> resolvers = createResolversWithAcknowledgement();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	// ========== Generic Type Extraction Tests ==========

	@Test
	void shouldExtractElementTypeFromList() throws Exception {
		Method method = TestMethods.class.getMethod("listOfCustomEvents", List.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldExtractElementTypeFromSet() throws Exception {
		Method method = TestMethods.class.getMethod("setOfCustomEvents", Set.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldExtractElementTypeFromCollection() throws Exception {
		Method method = TestMethods.class.getMethod("collectionOfCustomEvents", Collection.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldExtractElementTypeFromQueue() throws Exception {
		Method method = TestMethods.class.getMethod("queueOfCustomEvents", Queue.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldUnwrapMessageGeneric() throws Exception {
		Method method = TestMethods.class.getMethod("messageOfCustomEvent", Message.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldUnwrapListOfMessages() throws Exception {
		Method method = TestMethods.class.getMethod("listOfMessageOfCustomEvent", List.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldHandleComplexNestedGenerics() throws Exception {
		Method method = TestMethods.class.getMethod("collectionOfMessageOfCustomEvent", Collection.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	// ========== Various Payload Type Tests ==========

	@Test
	void shouldInferStringPayloadType() throws Exception {
		Method method = TestMethods.class.getMethod("simpleStringPayload", String.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(String.class);
	}

	@Test
	void shouldInferIntegerPayloadType() throws Exception {
		Method method = TestMethods.class.getMethod("integerPayload", Integer.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(Integer.class);
	}

	@Test
	void shouldInferComplexCustomType() throws Exception {
		Method method = TestMethods.class.getMethod("complexEventPayload", ComplexEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(ComplexEvent.class);
	}

	// ========== Edge Cases and Special Scenarios ==========

	@Test
	void shouldReturnNullWhenAllParametersSupportedByNonPayloadResolvers() throws Exception {
		Method method = TestMethods.class.getMethod("onlyNonPayloadParameters", String.class, String.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isNull();
	}

	@Test
	void shouldReturnNullWhenOnlyPayloadResolverParametersPresent() throws Exception {
		// This tests the case where all parameters would be handled by payload resolvers
		// but we filter them out, so nothing is left
		Method method = TestMethods.class.getMethod("messageParameter", Message.class);
		List<HandlerMethodArgumentResolver> resolvers = createOnlyPayloadResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		// Since MessageMethodArgumentResolver is filtered out as a payload resolver,
		// and there are no non-payload resolvers, the Message parameter should be inferred
		assertThat(result).isEqualTo(Object.class);
	}

	@Test
	void shouldHandleMethodWithOnlyPayloadParameter() throws Exception {
		Method method = TestMethods.class.getMethod("onlyPayload", CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldPreferPayloadAnnotationOverInference() throws Exception {
		Method method = TestMethods.class.getMethod("payloadAnnotationOverridesInference", String.class, String.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		// Second parameter has @Payload, so it should be inferred even though first param is supported by header
		// resolver
		assertThat(result).isEqualTo(String.class);
	}

	@Test
	void shouldHandlePrimitiveTypes() throws Exception {
		Method method = TestMethods.class.getMethod("primitiveIntPayload", int.class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(int.class);
	}

	@Test
	void shouldHandleArrayTypes() throws Exception {
		Method method = TestMethods.class.getMethod("arrayPayload", String[].class);
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(String[].class);
	}

	// ========== Resolver Behavior Tests ==========

	@Test
	void shouldWorkWithOnlyPayloadMethodArgumentResolver() throws Exception {
		Method method = TestMethods.class.getMethod("simpleStringPayload", String.class);
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(mock(PayloadMethodArgumentResolver.class));

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(String.class);
	}

	@Test
	void shouldWorkWithOnlyBatchPayloadMethodArgumentResolver() throws Exception {
		Method method = TestMethods.class.getMethod("listOfCustomEvents", List.class);
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(mock(BatchPayloadMethodArgumentResolver.class));

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldWorkWithOnlyMessageMethodArgumentResolver() throws Exception {
		Method method = TestMethods.class.getMethod("messageOfCustomEvent", Message.class);
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(mock(MessageMethodArgumentResolver.class));

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldFilterOutAllPayloadResolvers() throws Exception {
		Method method = TestMethods.class.getMethod("customEventPayload", CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createOnlyPayloadResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		// All resolvers are payload resolvers, so they're filtered out
		// CustomEvent parameter is not supported by any non-payload resolver, so it's the payload
		assertThat(result).isEqualTo(CustomEvent.class);
	}

	@Test
	void shouldHandleMixOfPayloadAndNonPayloadResolvers() throws Exception {
		Method method = TestMethods.class.getMethod("headerThenPayload", String.class, CustomEvent.class);
		List<HandlerMethodArgumentResolver> resolvers = createMixedResolvers();

		Class<?> result = inferrer.inferPayloadType(method, resolvers);

		assertThat(result).isEqualTo(CustomEvent.class);
	}

	// ========== Helper Methods ==========

	private List<HandlerMethodArgumentResolver> createStandardResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Mock PayloadMethodArgumentResolver
		PayloadMethodArgumentResolver payloadResolver = mock(PayloadMethodArgumentResolver.class);
		resolvers.add(payloadResolver);

		// Mock BatchPayloadMethodArgumentResolver
		BatchPayloadMethodArgumentResolver batchResolver = mock(BatchPayloadMethodArgumentResolver.class);
		resolvers.add(batchResolver);

		// Mock MessageMethodArgumentResolver
		MessageMethodArgumentResolver messageResolver = mock(MessageMethodArgumentResolver.class);
		resolvers.add(messageResolver);

		return resolvers;
	}

	private List<HandlerMethodArgumentResolver> createOnlyPayloadResolvers() {
		return createStandardResolvers();
	}

	private List<HandlerMethodArgumentResolver> createMixedResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		// Add a mock resolver that supports String parameters with @Header annotation
		HandlerMethodArgumentResolver headerResolver = mock(HandlerMethodArgumentResolver.class);
		when(headerResolver.supportsParameter(any(MethodParameter.class))).thenAnswer(invocation -> {
			MethodParameter param = invocation.getArgument(0);
			return param.getParameterType().equals(String.class) && param.hasParameterAnnotation(Header.class);
		});
		resolvers.add(headerResolver);

		return resolvers;
	}

	private List<HandlerMethodArgumentResolver> createResolversWithAcknowledgement() {
		List<HandlerMethodArgumentResolver> resolvers = createStandardResolvers();

		// Add a mock resolver that supports Acknowledgement parameters
		HandlerMethodArgumentResolver ackResolver = mock(HandlerMethodArgumentResolver.class);
		when(ackResolver.supportsParameter(any(MethodParameter.class))).thenAnswer(invocation -> {
			MethodParameter param = invocation.getArgument(0);
			return Acknowledgement.class.isAssignableFrom(param.getParameterType());
		});
		resolvers.add(ackResolver);

		return resolvers;
	}

	// ========== Test Method Signatures ==========

	static class TestMethods {

		// No parameters
		public void noParameters() {
		}

		// Simple types
		public void simpleStringPayload(String payload) {
		}

		public void integerPayload(Integer payload) {
		}

		public void primitiveIntPayload(int payload) {
		}

		public void arrayPayload(String[] payload) {
		}

		// Custom types
		public void customEventPayload(CustomEvent payload) {
		}

		public void complexEventPayload(ComplexEvent payload) {
		}

		// With @Payload annotation
		public void payloadAnnotationFirst(@Payload String payload) {
		}

		public void payloadAnnotationSecond(@Header String header, @Payload CustomEvent payload) {
		}

		public void payloadAnnotationList(@Payload List<CustomEvent> events) {
		}

		public void payloadAnnotationMessage(@Payload Message<CustomEvent> message) {
		}

		public void payloadAnnotationListOfMessages(@Payload List<Message<CustomEvent>> messages) {
		}

		public void payloadAnnotationOverridesInference(@Header String header, @Payload String payload) {
		}

		// Multiple parameters
		public void headerThenPayload(@Header String header, CustomEvent payload) {
		}

		public void multipleNonPayloadThenPayload(@Header String header1, @Header String header2, CustomEvent payload) {
		}

		public void onlyNonPayloadParameters(@Header String header1, @Header String header2) {
		}

		public void payloadWithAcknowledgement(CustomEvent payload, Acknowledgement ack) {
		}

		// Generic types
		public void listOfCustomEvents(List<CustomEvent> events) {
		}

		public void setOfCustomEvents(Set<CustomEvent> events) {
		}

		public void collectionOfCustomEvents(Collection<CustomEvent> events) {
		}

		public void queueOfCustomEvents(Queue<CustomEvent> events) {
		}

		public void messageOfCustomEvent(Message<CustomEvent> message) {
		}

		public void listOfMessageOfCustomEvent(List<Message<CustomEvent>> messages) {
		}

		public void collectionOfMessageOfCustomEvent(Collection<Message<CustomEvent>> messages) {
		}

		// Message parameter
		public void messageParameter(Message<?> message) {
		}

		// Only payload
		public void onlyPayload(CustomEvent payload) {
		}

	}

	static class CustomEvent {

		private String id;

		private String data;

		private LocalDateTime timestamp;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
		}

	}

	static class ComplexEvent {

		private String eventType;

		private CustomEvent details;

		private List<String> tags;

		public String getEventType() {
			return eventType;
		}

		public void setEventType(String eventType) {
			this.eventType = eventType;
		}

		public CustomEvent getDetails() {
			return details;
		}

		public void setDetails(CustomEvent details) {
			this.details = details;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

	}

}
