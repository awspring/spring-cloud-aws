/*
 * Copyright 2013-2025 the original author or authors.
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

import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;

/**
 * Represents a {@link AbstractPipelineMessageListenerContainer} component that can be observed.
 *
 * @author Tomaz Fernandes
 * @since 3.4
 */
public interface ObservableComponent {

	/**
	 * Set the Observation-related instances that are specific to a messaging system.
	 * @param observationSpecifics the observation-related instances.
	 */
	void setObservationSpecifics(AbstractListenerObservation.Specifics<?> observationSpecifics);

}
