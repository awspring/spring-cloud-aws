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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AlwaysAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.NeverAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.OnSuccessAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import java.util.Collection;

/**
 * A factory for creating components for the {@link MessageListenerContainer}. Implementations can instantiate and
 * configure each component according to its strategies, using the provided {@link ContainerOptions}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContainerComponentFactory<T> {

	/**
	 * Whether this factory supports the given queues based on the queue names.
	 * @param queueNames the queueNames.
	 * @param options {@link ContainerOptions} instance for evaluating support.
	 * @return true if the queues are supported.
	 */
	default boolean supports(Collection<String> queueNames, ContainerOptions options) {
		return true;
	}

	/**
	 * Create a {@link MessageSource} instance.
	 * @param options {@link ContainerOptions} instance for determining instance type and configuring.
	 * @return the instance.
	 */
	MessageSource<T> createMessageSource(ContainerOptions options);

	/**
	 * Create a {@link MessageSink} instance.
	 * @param options {@link ContainerOptions} instance for determining instance type and configuring.
	 * @return the instance.
	 */
	MessageSink<T> createMessageSink(ContainerOptions options);

	/**
	 * Create an {@link AcknowledgementProcessor} instance.
	 * @param options {@link ContainerOptions} instance for determining instance type and configuring.
	 * @return the instance.
	 */
	default AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options) {
		throw new UnsupportedOperationException("AcknowledgementProcessor support not implemented by this "
				+ ContainerComponentFactory.class.getSimpleName());
	}

	// @formatter:off

	/**
	 * Create a {@link AcknowledgementHandler} instance based on the given {@link ContainerOptions}
	 * @param options the {@link ContainerOptions} instance
	 * @return the instance.
	 */
	default AcknowledgementHandler<T> createAcknowledgementHandler(ContainerOptions options) {
		AcknowledgementMode mode = options.getAcknowledgementMode();
		return AcknowledgementMode.ON_SUCCESS.equals(mode)
			? new OnSuccessAcknowledgementHandler<>()
			: AcknowledgementMode.ALWAYS.equals(mode)
				? new AlwaysAcknowledgementHandler<>()
				: new NeverAcknowledgementHandler<>();
	}
	// @formatter:on

}
