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

package org.springframework.cloud.aws.messaging.listener.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * Annotation for mapping a {@link org.springframework.messaging.Message} onto listener
 * methods by matching to the message destination. The destination can be a logical queue
 * name (CloudFormation), a physical queue name or a queue URL.
 * <p>
 * Listener methods which are annotated with this annotation are allowed to have flexible
 * signatures. They may have arguments of the following types, in arbitrary order:
 * <ul>
 * <li>{@link org.springframework.messaging.Message} to get access to the complete message
 * being processed.</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Payload}-annotated method
 * arguments to extract the payload of a message and optionally convert it using a
 * {@link org.springframework.messaging.converter.MessageConverter}. The presence of the
 * annotation is not required since it is assumed by default for method arguments that are
 * not annotated.
 * <li>{@link org.springframework.messaging.handler.annotation.Header}-annotated method
 * arguments to extract a specific header value along with type conversion with a
 * {@link org.springframework.core.convert.converter.Converter} if necessary.</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Headers}-annotated argument
 * that must also be assignable to {@link java.util.Map} for getting access to all
 * headers.</li>
 * <li>{@link org.springframework.messaging.MessageHeaders} arguments for getting access
 * to all headers.</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor}</li>
 * <li>{@link org.springframework.cloud.aws.messaging.listener.Acknowledgment} to be able
 * to acknowledge the reception of a message an trigger the deletion of it. This argument
 * is only available when using the deletion policy
 * {@link SqsMessageDeletionPolicy#NEVER}.</li>
 * </ul>
 * <p>
 * Additionally a deletion policy can be chosen to define when a message must be deleted
 * once the listener method has been called. To get an overview of the available deletion
 * policies read the {@link SqsMessageDeletionPolicy} documentation.
 * </p>
 * <p>
 * By default the return value is wrapped as a message and sent to the destination
 * specified with an
 * {@link org.springframework.messaging.handler.annotation.SendTo @SendTo} method-level
 * annotation.
 *
 * @author Alain Sahli
 * @since 1.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MessageMapping
public @interface SqsListener {

	/**
	 * List of queues. Queues can be defined by their logical/physical name or URL.
	 * @return list of queues
	 */
	String[] value() default {};

	/**
	 * Defines the deletion policy that must be applied once the listener method was
	 * called.
	 * @return deletion policy
	 */
	SqsMessageDeletionPolicy deletionPolicy() default SqsMessageDeletionPolicy.NO_REDRIVE;

}
