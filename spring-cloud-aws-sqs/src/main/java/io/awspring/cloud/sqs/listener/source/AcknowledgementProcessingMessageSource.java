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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;

/**
 * {@link MessageSource} specialization that enables processing acknowledgements for the
 * {@link org.springframework.messaging.Message} instances through an
 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementExecutor}
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AcknowledgementProcessingMessageSource<T> extends MessageSource<T> {

	/**
	 * Set the {@link AcknowledgementProcessor} instance that will process the message instances and provide the
	 * {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback}.
	 * @param acknowledgementProcessor the processor instance.
	 */
	void setAcknowledgementProcessor(AcknowledgementProcessor<T> acknowledgementProcessor);

	/**
	 * Set the {@link AsyncAcknowledgementResultCallback} that will be executed after messages are acknowledged, usually
	 * by a {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementExecutor}.
	 * @param acknowledgementResultCallback the callback instance.
	 */
	void setAcknowledgementResultCallback(AsyncAcknowledgementResultCallback<T> acknowledgementResultCallback);

}
