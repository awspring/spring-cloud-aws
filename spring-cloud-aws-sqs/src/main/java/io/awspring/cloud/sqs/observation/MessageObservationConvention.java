/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import org.springframework.lang.Nullable;

/**
 * {@link ObservationConvention} interface for SQS message operations.
 *
 * @author Mariusz Sondecki
 */
public interface MessageObservationConvention<T extends Observation.Context> extends ObservationConvention<T> {

	@Nullable
	String getMessageId(T context);

	MessagingOperationType getMessageType();

	default String getOrDefault(String value) {
		if (value == null) {
			return KeyValue.NONE_VALUE;
		}
		return value;
	}

	default KeyValue getId(T context) {
		String messageId = getOrDefault(getMessageId(context));
		return KeyValue.of(MessageObservationDocumentation.HighCardinalityKeyNames.MESSAGE_ID, messageId);
	}

	@Override
	default String getContextualName(T context) {
		return "%s %s".formatted(getId(context).getValue(), getMessageType().getValue());
	}

	@Override
	default KeyValues getHighCardinalityKeyValues(T context) {
		return KeyValues.of(getId(context));
	}

	@Override
	default KeyValues getLowCardinalityKeyValues(T context) {
		return KeyValues.of(KeyValue.of(MessageObservationDocumentation.LowCardinalityKeyNames.OPERATION,
				getMessageType().getValue()));
	}

	@Override
	default String getName() {
		return "sqs." + getMessageType().getValue().replace(" ", ".");
	}
}
