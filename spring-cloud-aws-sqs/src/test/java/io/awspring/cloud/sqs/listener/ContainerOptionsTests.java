/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class ContainerOptionsTests {

	@Test
	void shouldDefaultToCreateQueues() {
		ContainerOptions options = ContainerOptions.builder().build();
		assertThat(options.getQueueNotFoundStrategy()).isEqualTo(QueueNotFoundStrategy.CREATE);
	}

	@Test
	void shouldHaveSameValuesAfterBuilder() {
		ContainerOptions options = ContainerOptions.builder().build();
		ContainerOptions builtCopy = options.toBuilder().build();
		assertThat(options).usingRecursiveComparison().isEqualTo(builtCopy);
	}

	@Test
	void shouldCreateCopy() {
		ContainerOptions options = ContainerOptions.builder().build();
		ContainerOptions copy = options.createCopy();
		assertThat(options).usingRecursiveComparison().isEqualTo(copy);
	}

	@Test
	void shouldCreateCopyOfBuilder() {
		ContainerOptions.Builder builder = ContainerOptions.builder();
		ContainerOptions.Builder copy = builder.createCopy();
		assertThat(copy).usingRecursiveComparison().isEqualTo(builder);
	}

	@Test
	void shouldHaveSameFieldsInBuilder() {
		ContainerOptions options = ContainerOptions.builder().build();
		ContainerOptions.Builder builtCopy = options.toBuilder();
		assertThat(options).usingRecursiveComparison().isEqualTo(builtCopy);
	}

	@Test
	void shouldSetMessageAttributeNames() {
		List<String> messageAttributeNames = Arrays.asList("name-1", "name-2");
		ContainerOptions options = ContainerOptions.builder().messageAttributeNames(messageAttributeNames).build();
		assertThat(options.getMessageAttributeNames()).containsExactlyElementsOf(messageAttributeNames);
	}

	@Test
	void shouldSetMessageSystemAttributeNames() {
		List<MessageSystemAttributeName> attributeNames = Arrays.asList(MessageSystemAttributeName.MESSAGE_GROUP_ID,
				MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID);
		ContainerOptions options = ContainerOptions.builder().messageSystemAttributeNames(attributeNames).build();
		assertThat(options.getMessageSystemAttributeNames()).containsExactlyInAnyOrderElementsOf(
				attributeNames.stream().map(MessageSystemAttributeName::toString).collect(Collectors.toList()));
	}

	@Test
	void shouldSetTaskExecutor() {
		TaskExecutor executor = mock(TaskExecutor.class);
		ContainerOptions options = ContainerOptions.builder().componentsTaskExecutor(executor).build();
		assertThat(options.getComponentsTaskExecutor()).isEqualTo(executor);
	}

	@Test
	void shouldSetQueueNotFoundStrategy() {
		ContainerOptions options = ContainerOptions.builder().queueNotFoundStrategy(QueueNotFoundStrategy.FAIL).build();
		assertThat(options.getQueueNotFoundStrategy()).isEqualTo(QueueNotFoundStrategy.FAIL);
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldSetMessageConverter() {
		MessagingMessageConverter<Object> converter = mock(MessagingMessageConverter.class);
		ContainerOptions options = ContainerOptions.builder().messageConverter(converter).build();
		assertThat(options.getMessageConverter()).isEqualTo(converter);
	}

}
