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

import io.micrometer.observation.transport.ReceiverContext;
import java.util.Collection;
import org.springframework.messaging.MessageHeaders;

/**
 * Context that holds information for observation metadata collection during the
 * {@link MessageObservationDocumentation#BATCH_MESSAGE_POLLING_PROCESS processing of the whole received batch of SQS
 * messages}.
 * <p>
 * The inbound tracing information is propagated in the form of a list of {@link io.micrometer.tracing.Link} by looking
 * it up in {@link MessageHeaders#get(Object, Class) incoming SQS message headers} of the entire received batch.
 *
 * @author Mariusz Sondecki
 */
public class BatchMessagePollingProcessObservationContext extends ReceiverContext<Collection<MessageHeaders>> {

	public BatchMessagePollingProcessObservationContext(Collection<MessageHeaders> messageHeaders) {
		super((carrier, key) -> null);
		setCarrier(messageHeaders);
	}

}
