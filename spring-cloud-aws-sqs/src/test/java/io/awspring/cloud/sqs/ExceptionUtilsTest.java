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
package io.awspring.cloud.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExceptionUtils}.
 *
 * @author Tomaz Fernandes
 */
class ExceptionUtilsTest {

	@Test
	void shouldReturnOriginalExceptionWhenNoExceptionsToUnwrap() {
		// given
		RuntimeException original = new RuntimeException("Original exception");

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(original);

		// then
		assertThat(unwrapped).isSameAs(original);
	}

	@Test
	void shouldReturnOriginalExceptionWhenNotInExceptionsToUnwrap() {
		// given
		RuntimeException original = new RuntimeException("Original exception");

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(original, IOException.class);

		// then
		assertThat(unwrapped).isSameAs(original);
	}

	@Test
	void shouldUnwrapSingleLevelException() {
		// given
		IOException cause = new IOException("Root cause");
		CompletionException original = new CompletionException(cause);

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(original, CompletionException.class);

		// then
		assertThat(unwrapped).isSameAs(cause);
	}

	@Test
	void shouldUnwrapMultipleLevelExceptions() {
		// given
		IllegalArgumentException rootCause = new IllegalArgumentException("Root cause");
		IOException midCause = new IOException("Mid cause", rootCause);
		CompletionException executionException = new CompletionException(midCause);
		RuntimeException topException = new RuntimeException("Top exception", executionException);

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(topException, RuntimeException.class,
				CompletionException.class);

		// then
		assertThat(unwrapped).isSameAs(midCause);
	}

	@Test
	void shouldUnwrapAllSpecifiedExceptionTypes() {
		// given
		IllegalArgumentException rootCause = new IllegalArgumentException("Root cause");
		ExecutionException midCause1 = new ExecutionException("Mid cause 1", rootCause);
		CompletionException midCause2 = new CompletionException(midCause1);
		RuntimeException topException = new RuntimeException("Top exception", midCause2);

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(topException, RuntimeException.class,
				CompletionException.class, ExecutionException.class);

		// then
		assertThat(unwrapped).isSameAs(rootCause);
	}

	@Test
	void shouldHandleNullCause() {
		// given
		CompletionException exceptionWithNullCause = new CompletionException(null);

		// when/then
		Throwable unwrapped = ExceptionUtils.unwrapException(exceptionWithNullCause, CompletionException.class);

		// then
		assertThat(unwrapped).isEqualTo(exceptionWithNullCause);
	}

	@Test
	void shouldWorkWithCustomExceptions() {
		// given
		class Level1Exception extends RuntimeException {
			Level1Exception(Throwable cause) {
				super(cause);
			}
		}

		class Level2Exception extends RuntimeException {
			Level2Exception(Throwable cause) {
				super(cause);
			}
		}

		IOException rootCause = new IOException("Root cause");
		Level2Exception level2 = new Level2Exception(rootCause);
		Level1Exception level1 = new Level1Exception(level2);

		// when
		Throwable unwrapped = ExceptionUtils.unwrapException(level1, Level1Exception.class, Level2Exception.class);

		// then
		assertThat(unwrapped).isSameAs(rootCause);
	}
}
