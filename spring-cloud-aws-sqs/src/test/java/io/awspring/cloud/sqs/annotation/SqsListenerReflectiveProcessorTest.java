/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.stereotype.Service;

public class SqsListenerReflectiveProcessorTest {

	private final SqsListenerReflectiveProcessor processor = new SqsListenerReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerClassAndListenerParameterForReflection() throws NoSuchMethodException {
		Method method = MyService.class.getDeclaredMethod("listen", MyService.SampleRecord.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(MyService.class)), typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(MyService.SampleRecord.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS);
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							hint -> assertThat(hint.getName()).isEqualTo("propertyOne"),
							hint -> assertThat(hint.getName()).isEqualTo("propertyTwo"));
				}, typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)));
	}

	@Service
	public class MyService {

		@SqsListener(queueNames = "myQueueName")
		public void listen(SampleRecord message) {
		}

		private record SampleRecord(String propertyOne, String propertyTwo) {
		}

	}
}
