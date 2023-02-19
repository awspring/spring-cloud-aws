/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;

/**
 * Acknowledgement modes to be used by a {@link org.springframework.messaging.core.MessageReceivingOperations}
 * implementation.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public enum TemplateAcknowledgementMode {

	/**
	 * Don't acknowledge messages automatically. The message or messages can be acknowledged later with
	 * {@link io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement#acknowledge} or
	 * {@link Acknowledgement#acknowledgeAsync()}
	 */
	MANUAL,

	/**
	 * Acknowledge received messages. Any exceptions that might occur in the acknowledging process are wrapped and
	 * rethrown.
	 */
	ACKNOWLEDGE

}
