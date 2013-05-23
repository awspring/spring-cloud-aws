/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core;

/**
 * Simple operations interface to send notification to the Amazon Webservices Simple Notification Service. This
 * interface enables clients to send regular java object without the need for a conversation into messaging
 * infrastructure specific object. In contrast to {@link QueueingOperations} this interface does not support any method
 * to retrieve notification as this is not supported by the notification system (besides sending notification through
 * an Amazon SQS queue).
 *
 * @author Agim Emruli
 * @see org.elasticspring.messaging.core.sns.SimpleNotificationServiceTemplate
 * @since 1.0
 */
public interface NotificationOperations {

	/**
	 * Converts the payload into the notification specific messaging format and sends the notification to the Amazon SNS
	 * system. The payload will become the payload of the message itself. The subject of the notification will be null in
	 * this case. The object might be any object that can be serialized from the implementation into the notification
	 * specific format. This method does not assume any destination as this will be normally a static configured one from
	 * the implementation.
	 *
	 * @param payload
	 * 		- the payload object, must not be null
	 */
	void convertAndSend(Object payload);

	/**
	 * Send the notification with the same semantics like {@link #convertAndSend(Object)} but also sends a subject for the
	 * notification. The subject is an additional information for the notification which might be processed by the
	 * notification consumer.
	 *
	 * @param payload
	 * 		- the payload object, must not be null
	 * @param subject
	 * 		- the subject for the notification, can be null
	 */
	void convertAndSendWithSubject(Object payload, String subject);

	/**
	 * Send the notification to the specified destination name which will be the canonical or logical name for the topic
	 * to
	 * which the message is send. The destination name will be resolved before the notification is actually sent.
	 *
	 * @param destinationName
	 * 		- the logical or canonical destination name
	 * @param payload
	 * 		-  the payload to be send for the notification
	 */
	void convertAndSend(String destinationName, Object payload);

	/**
	 * Send the notification with the same semantics like {@link #convertAndSend(String, Object)} but with additional
	 * support for notification subjects.
	 *
	 * @param destinationName
	 * 		- the logical or canonical destination name
	 * @param payload
	 * 		-  the payload to be send for the notification
	 * @param subject
	 * 		- the subject for the notification, can be null
	 */
	void convertAndSendWithSubject(String destinationName, Object payload, String subject);
}
