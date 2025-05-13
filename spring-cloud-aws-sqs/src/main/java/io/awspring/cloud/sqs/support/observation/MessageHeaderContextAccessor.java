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
package io.awspring.cloud.sqs.support.observation;

import io.micrometer.context.ContextAccessor;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageHeaders;

/**
 * {@link ContextAccessor} that use {@link MessageHeaders} to hold the Observation context. Intended for read-only
 * operations.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public class MessageHeaderContextAccessor implements ContextAccessor<MessageHeaders, MessageHeaders> {

	@Override
	@NonNull
	public Class<? extends MessageHeaders> readableType() {
		return MessageHeaders.class;
	}

	@Override
	public void readValues(MessageHeaders sourceContext, @NonNull Predicate<Object> keyPredicate,
			@NonNull Map<Object, Object> target) {
		sourceContext.forEach((k, v) -> {
			if (keyPredicate.test(k)) {
				target.put(k, v);
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T readValue(MessageHeaders sourceContext, Object key) {
		return (T) sourceContext.get(key.toString());
	}

	@Override
	@NonNull
	public Class<? extends MessageHeaders> writeableType() {
		return MessageHeaders.class;
	}

	@Override
	@NonNull
	public MessageHeaders writeValues(@NonNull Map<Object, Object> valuesToWrite,
			@NonNull MessageHeaders targetContext) {
		throw new UnsupportedOperationException("Should not write values to the MessageHeaders");
	}
}
