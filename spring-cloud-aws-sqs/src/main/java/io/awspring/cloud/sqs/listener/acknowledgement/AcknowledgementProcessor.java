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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.listener.ConfigurableContainerComponent;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.IdentifiableContainerComponent;
import org.springframework.context.SmartLifecycle;

/**
 * Top-level interface for a component capable of processing acknowledgements. Provides the
 * {@link #getAcknowledgementCallback()} method that allows offering messages to the processor.
 *
 * The timing of the execution of the acknowledgements depends on many factors sucha as
 * {@link ContainerOptions#getAcknowledgementInterval()}, {@link ContainerOptions#getAcknowledgementThreshold()},
 * {@link ContainerOptions#getAcknowledgementOrdering()}.
 *
 * The actual execution is usually handled by an {@link AcknowledgementExecutor}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see ImmediateAcknowledgementProcessor
 * @see BatchingAcknowledgementProcessor
 * @see ExecutingAcknowledgementProcessor
 * @see SqsAcknowledgementExecutor
 */
public interface AcknowledgementProcessor<T>
		extends SmartLifecycle, IdentifiableContainerComponent, ConfigurableContainerComponent {

	/**
	 * Retrieve an acknowledgement callback that can be used to offer messages to be acknowledged by this processor.
	 * @return the callback.
	 */
	AcknowledgementCallback<T> getAcknowledgementCallback();

}
