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

/**
 * Defines the policy that must be used for the deletion of SQS messages once they were
 * processed. The deletion policy can be set individually on every listener method using
 * the
 * {@link org.springframework.cloud.aws.messaging.listener.annotation.SqsListener @SqsListener}
 * annotation. The default policy is {@code NO_REDRIVE} because it is the safest way to
 * avoid poison messages and have a safe way to avoid the loss of messages (i.e. using a
 * dead letter queue).
 * <p>
 * The following deletion policies are available:
 * <ul>
 * <li><b>ALWAYS</b>: Always deletes message in case of success (no exception thrown) or
 * failure (exception thrown) during message processing by the listener method.</li>
 * <li><b>NEVER</b>: Never deletes message automatically. The receiving listener method
 * must acknowledge each message manually by using the acknowledgment parameter.</li>
 * <li><b>NO_REDRIVE</b>: Deletes message if no redrive policy is defined.</li>
 * <li><b>ON_SUCCESS</b>: Deletes message when successfully executed by the listener
 * method. If an exception is thrown by the listener method, the message will not be
 * deleted.</li>
 * </ul>
 * </p>
 *
 * @author Alain Sahli
 * @since 1.1
 * @see org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
 */
public enum SqsMessageDeletionPolicy {

	/**
	 * Always deletes message in case of success (no exception thrown) or failure
	 * (exception thrown) during message processing by the listener method.
	 */
	ALWAYS,

	/**
	 * Never deletes message automatically. The receiving listener method must acknowledge
	 * each message manually by using the acknowledgment parameter.
	 * <p>
	 * <b>IMPORTANT</b>: When using this policy the listener method must take care of the
	 * deletion of the messages. If not, it will lead to an endless loop of messages
	 * (poison messages).
	 * </p>
	 *
	 * @see Acknowledgment
	 */
	NEVER,

	/**
	 * Deletes message if no redrive policy is defined.
	 */
	NO_REDRIVE,

	/**
	 * Deletes message when successfully executed by the listener method. If an exception
	 * is thrown by the listener method, the message will not be deleted.
	 */
	ON_SUCCESS

}
