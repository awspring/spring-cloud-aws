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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultListenerContainerRegistry}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class DefaultListenerContainerRegistryTests {

	@Test
	void shouldRegisterListenerContainer() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container);
		assertThat(registry.getPhase()).isEqualTo(MessageListenerContainer.DEFAULT_PHASE);
	}

	@Test
	void shouldGetListenerContainer() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setPhase(2);
		registry.registerListenerContainer(container);
		MessageListenerContainer<?> containerFromRegistry = registry.getContainerById(id);
		assertThat(containerFromRegistry).isEqualTo(container);
		assertThat(registry.getPhase()).isEqualTo(2);
	}

	@Test
	void shouldGetAllListenerContainers() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container2 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container3 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		String id2 = "test-container-id-2";
		String id3 = "test-container-id-3";
		given(container1.getId()).willReturn(id1);
		given(container2.getId()).willReturn(id2);
		given(container3.getId()).willReturn(id3);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.registerListenerContainer(container2);
		registry.registerListenerContainer(container3);
		assertThat(registry.getListenerContainers()).containsExactlyInAnyOrder(container1, container2, container3);
	}

	@Test
	void shouldStartAndStopAllListenerContainers() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container2 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container3 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		String id2 = "test-container-id-2";
		String id3 = "test-container-id-3";
		given(container1.getId()).willReturn(id1);
		given(container1.isAutoStartup()).willReturn(true);
		given(container2.getId()).willReturn(id2);
		given(container2.isAutoStartup()).willReturn(true);
		given(container3.getId()).willReturn(id3);
		given(container3.isAutoStartup()).willReturn(true);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.registerListenerContainer(container2);
		registry.registerListenerContainer(container3);
		registry.start();
		assertThat(registry.isRunning()).isTrue();
		registry.stop();
		assertThat(registry.isRunning()).isFalse();
		then(container1).should().start();
		then(container1).should().stop();
		then(container2).should().start();
		then(container2).should().stop();
		then(container3).should().start();
		then(container3).should().stop();
	}

	@Test
	void shouldNotStartContainerWithAutoStartupFalse() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		given(container1.getId()).willReturn(id1);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.start();
		assertThat(registry.isRunning()).isTrue();
		registry.stop();
		assertThat(registry.isRunning()).isFalse();
		then(container1).should(times(0)).start();
		then(container1).should().stop();
	}

	@Test
	void shouldThrowIfIdAlreadyPresent() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container);
		assertThatThrownBy(() -> registry.registerListenerContainer(container))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
