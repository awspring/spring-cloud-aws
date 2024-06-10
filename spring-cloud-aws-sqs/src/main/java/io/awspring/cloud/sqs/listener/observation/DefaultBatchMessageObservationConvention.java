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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.MessageHeaders;

/**
 * Default implementation for {@link BatchMessageObservationConvention}.
 *
 * @author Mariusz Sondecki
 */
public class DefaultBatchMessageObservationConvention implements BatchMessageObservationConvention {

	private static final KeyValue BATCH_PROCESSING_MODE = KeyValue
			.of(MessageObservationDocumentation.LowCardinalityKeyNames.PROCESSING_MODE, "batch");

	@Override
	public String getName() {
		return "sqs.batch.message.process";
	}

	@Override
	public String getMessageId(BatchMessageObservationContext context) {
		return context.getCarrier().stream().map(MessageHeaders::getId).filter(Objects::nonNull).map(UUID::toString)
				.collect(Collectors.joining("; "));
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(BatchMessageObservationContext context) {
		return KeyValues.of(BATCH_PROCESSING_MODE);
	}
}
