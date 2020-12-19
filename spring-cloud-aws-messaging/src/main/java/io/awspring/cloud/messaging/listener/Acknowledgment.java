/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.listener;

import java.util.concurrent.Future;

/**
 * Acknowledgment interface that can be injected as parameter into a listener method. The
 * purpose of this acknowledgment is to provide a way for the listener methods to
 * acknowledge the reception and processing of a message. The call to the
 * {@link Acknowledgment#acknowledge} method triggers some implementation specific clean
 * up tasks like removing a message from the SQS queue. The
 * {@link Acknowledgment#acknowledge} method returns a {@link Future} as the
 * acknowledgment can involve some asynchronous request to an AWS API.
 *
 * @author Alain Sahli
 * @since 1.1
 */
public interface Acknowledgment {

	/**
	 * The call to this method acknowledges the caller that the listener method has
	 * finished the processing of the message and triggers some implementation specific
	 * clean up tasks like removing a message from the SQS queue.
	 * @return a {@link Future} as the acknowledgment can involve some asynchronous
	 * request (i.e. request to an AWS API).
	 */
	Future<?> acknowledge();

}
