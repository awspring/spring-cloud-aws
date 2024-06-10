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
package io.awspring.cloud.sqs.listener.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import java.util.UUID;

/**
 * Default implementation for {@link SingleMessageObservationConvention}.
 *
 * @author Mariusz Sondecki
 */
public class DefaultSingleMessageObservationConvention implements SingleMessageObservationConvention {

	private static final KeyValue SINGLE_MESSAGE_PROCESSING_MODE = KeyValue
			.of(MessageObservationDocumentation.LowCardinalityKeyNames.PROCESSING_MODE, "single");

	@Override
	public String getName() {
		return "sqs.single.message.process";
	}

	@Override
	public String getMessageId(SingleMessageObservationContext context) {
		UUID id = context.getCarrier().getId();
		return id != null ? id.toString() : null;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(SingleMessageObservationContext context) {
		return KeyValues.of(SINGLE_MESSAGE_PROCESSING_MODE);
	}
}
