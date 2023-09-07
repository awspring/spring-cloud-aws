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
package io.awspring.cloud.sqs.annotation;

import io.awspring.cloud.sqs.listener.acknowledgement.handler.AlwaysAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.NeverAcknowledgementHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.OnSuccessAcknowledgementHandler;

/**
 * Configures the acknowledgement behavior for the container.
 *
 * @author Joao Calassio
 * @since 3.1
 * @see OnSuccessAcknowledgementHandler
 * @see AlwaysAcknowledgementHandler
 * @see NeverAcknowledgementHandler
 * @see io.awspring.cloud.sqs.listener.ContainerOptions
 * @see SqsListener
 */
public class SqsListenerAcknowledgementMode {

	/**
	 * Use acknowledge mode defined by the container.
	 */
	public static final String DEFAULT = "DEFAULT";

	/**
	 * Messages will be acknowledged when message processing is successful.
	 */
	public static final String ON_SUCCESS = "ON_SUCCESS";

	/**
	 * Messages will be acknowledged whether processing was completed successfully or with an error.
	 */
	public static final String ALWAYS = "ALWAYS";

	/**
	 * Messages will not be acknowledged automatically by the container.
	 * @see io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
	 */
	public static final String MANUAL = "MANUAL";

}
