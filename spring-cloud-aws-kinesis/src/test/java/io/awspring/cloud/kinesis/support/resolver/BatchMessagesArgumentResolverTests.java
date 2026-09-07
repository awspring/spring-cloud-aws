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
package io.awspring.cloud.kinesis.support.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

class BatchMessagesArgumentResolverTests {

	private final BatchMessagesArgumentResolver resolver = new BatchMessagesArgumentResolver(messageConverter());

	@Test
	@DisplayName("List<Pojo> parameter receives converted POJOs, not raw byte[]")
	@SuppressWarnings("unchecked")
	void convertsEachElementPayloadToPojo() {
		MethodParameter parameter = pojoListParameter();
		Message<?> batch = batchOf(json("first"), json("second"));

		Object resolved = resolver.resolveArgument(parameter, batch);

		assertThat(resolved).isInstanceOf(List.class);
		List<Person> people = (List<Person>) resolved;
		assertThat(people).extracting(Person::name).containsExactly("first", "second");
	}

	@Test
	@DisplayName("List<Message<Pojo>> parameter receives Messages whose payloads are converted POJOs")
	@SuppressWarnings("unchecked")
	void convertsMessagePayloadsToPojo() {
		MethodParameter parameter = messageListParameter();
		Message<?> batch = batchOf(json("alice"), json("bob"));

		Object resolved = resolver.resolveArgument(parameter, batch);

		assertThat(resolved).isInstanceOf(List.class);
		List<Message<Person>> messages = (List<Message<Person>>) resolved;
		assertThat(messages).extracting(m -> m.getPayload().name()).containsExactly("alice", "bob");
	}

	@Test
	@DisplayName("raw Collection<Message<?>> parameter passes the messages through unconverted")
	@SuppressWarnings("unchecked")
	void passesThroughWhenElementTypeIsMessage() {
		MethodParameter parameter = rawMessageListParameter();
		Message<?> batch = batchOf(json("x"), json("y"));

		Object resolved = resolver.resolveArgument(parameter, batch);

		assertThat((List<Message<?>>) resolved).hasSize(2);
	}

	private Message<byte[]> json(String name) {
		byte[] payload = ("{\"name\":\"" + name + "\"}").getBytes(StandardCharsets.UTF_8);
		return MessageBuilder.withPayload(payload)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON).build();
	}

	private Message<?> batchOf(Message<?>... elements) {
		List<Message<?>> messages = new ArrayList<>(List.of(elements));
		return MessageBuilder.withPayload(messages).build();
	}

	private MessageConverter messageConverter() {
		List<MessageConverter> converters = new ArrayList<>();
		converters.add(new ByteArrayMessageConverter());
		converters.add(new StringMessageConverter());
		JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter();
		jacksonConverter.setSerializedPayloadClass(String.class);
		jacksonConverter.setStrictContentTypeMatch(false);
		converters.add(jacksonConverter);
		converters.add(new SimpleMessageConverter());
		return new CompositeMessageConverter(converters);
	}

	private MethodParameter pojoListParameter() {
		return new MethodParameter(method("pojoList", List.class), 0);
	}

	private MethodParameter messageListParameter() {
		return new MethodParameter(method("messageList", List.class), 0);
	}

	private MethodParameter rawMessageListParameter() {
		return new MethodParameter(method("rawMessageList", List.class), 0);
	}

	private Method method(String name, Class<?>... parameterTypes) {
		try {
			return Objects.requireNonNull(ReflectionTarget.class.getDeclaredMethod(name, parameterTypes));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);
		}
	}

	record Person(String name) {
	}

	static final class ReflectionTarget {

		void pojoList(List<Person> people) {
		}

		void messageList(List<Message<Person>> messages) {
		}

		void rawMessageList(List<Message<?>> messages) {
		}

	}

}
