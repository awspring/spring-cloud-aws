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

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContainerComponentFactory<T> {

	MessageSource<T> createMessageSource(ContainerOptions options);

	MessageSink<T> createMessageSink(ContainerOptions options);

	AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options);

	// @formatter:off
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
