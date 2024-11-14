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

import io.micrometer.tracing.Link;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import java.util.function.Supplier;

/**
 * A TracingObservationHandler called when receiving occurred - e.g. batches of messages.
 *
 * @author Mariusz Sondecki
 */
public class BatchMessageProcessTracingObservationHandler
		extends PropagatingReceiverTracingObservationHandler<BatchMessagePollingProcessObservationContext> {

	private final Supplier<Propagator> propagatorSupplier;

	/**
	 * Creates a new instance of {@link BatchMessageProcessTracingObservationHandler}.
	 *
	 * @param tracer the tracer to use to record events
	 * @param propagator the mechanism to propagate tracing information from the carrier
	 */
	public BatchMessageProcessTracingObservationHandler(Tracer tracer, Propagator propagator) {
		super(tracer, propagator);
		this.propagatorSupplier = () -> propagator;
	}

	@Override
	public Span.Builder customizeExtractedSpan(BatchMessagePollingProcessObservationContext context,
			Span.Builder builder) {
		context.getCarrier().forEach(messageHeaders -> {
			TraceContext traceContext = this.propagatorSupplier.get()
					.extract(messageHeaders, (carrier, key) -> carrier.get(key, String.class)).start().context();
			String newParentSpanId = traceContext.parentId(); // newParentSpanId should be a span ID of received message
			if (newParentSpanId != null) {// add only when received message contains tracing information
				builder.addLink(new Link(traceContext));
			}
		});
		return super.customizeExtractedSpan(context, builder);
	}
}
