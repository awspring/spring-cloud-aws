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

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.MessageHeaders;

/**
 * Default implementation for {@link BatchMessagePublishObservationConvention}.
 *
 * @author Mariusz Sondecki
 */
public class DefaultBatchMessagePublishObservationConvention implements BatchMessagePublishObservationConvention {

	@Override
	public String getMessageId(BatchMessagePublishObservationContext context) {
		return context.getMessageHeaders().stream().map(MessageHeaders::getId).filter(Objects::nonNull)
				.map(UUID::toString).collect(Collectors.joining("; "));
	}

	@Override
	public MessagingOperationType getMessageType() {
		return MessagingOperationType.BATCH_PUBLISH;
	}
}
