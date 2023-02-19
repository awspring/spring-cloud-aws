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
package io.awspring.cloud.sqs.listener.acknowledgement.handler;

/**
 * Configures the acknowledgement behavior for this container.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see OnSuccessAcknowledgementHandler
 * @see AlwaysAcknowledgementHandler
 * @see NeverAcknowledgementHandler
 * @see io.awspring.cloud.sqs.listener.ContainerOptions
 */
public enum AcknowledgementMode {

	/**
	 * Messages will be acknowledged when message processing is successful.
	 */
	ON_SUCCESS,

	/**
	 * Messages will be acknowledged whether processing was completed successfully or with an error.
	 */
	ALWAYS,

	/**
	 * Messages will not be acknowledged automatically by the container.
	 * @see io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
	 */
	MANUAL

}
