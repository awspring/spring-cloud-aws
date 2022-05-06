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
package io.awspring.cloud.messaging.support;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessagingUtils {

	public static final MessagingUtils INSTANCE = new MessagingUtils();

	private MessagingUtils() {
	}

	public <T> MessagingUtils acceptIfNotNull(T value, Consumer<T> consumer) {
		if (value != null) {
			consumer.accept(value);
		}
		return this;
	}

	public <T, V> MessagingUtils acceptBothIfNoneNull(T firstValue, V secondValue, BiConsumer<T, V> consumer) {
		if (firstValue != null && secondValue != null) {
			consumer.accept(firstValue, secondValue);
		}
		return this;
	}

	public <T> MessagingUtils acceptFirstNonNull(Consumer<T> consumer, T... values) {
		Arrays.stream(values).filter(Objects::nonNull).findFirst().ifPresent(consumer);
		return this;
	}
}
