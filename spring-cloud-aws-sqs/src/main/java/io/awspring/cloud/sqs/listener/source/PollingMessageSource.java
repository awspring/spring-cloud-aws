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

import io.awspring.cloud.sqs.listener.TaskExecutorAware;
import io.awspring.cloud.sqs.listener.backpressure.BackPressureHandler;
import org.springframework.context.SmartLifecycle;

/**
 * {@link MessageSource} extension that provides polling configurations and {@link SmartLifecycle} capabilities.
 *
 * @param <T> the message payload type.
 */
public interface PollingMessageSource<T>
		extends AcknowledgementProcessingMessageSource<T>, SmartLifecycle, TaskExecutorAware {

	/**
	 * Set the endpoint name that will be polled by this source.
	 * @param endpointName the name.
	 */
	void setPollingEndpointName(String endpointName);

	/**
	 * Set the {@link BackPressureHandler} that will be use to handle backpressure in this source.
	 * @param backPressureHandler the handler.
	 */
	void setBackPressureHandler(BackPressureHandler backPressureHandler);

}
