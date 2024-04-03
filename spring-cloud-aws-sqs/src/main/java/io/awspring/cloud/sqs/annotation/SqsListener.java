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
package io.awspring.cloud.sqs.annotation;

import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/**
 * Methods with this annotation will be wrapped by a {@link io.awspring.cloud.sqs.listener.MessageListener} or
 * {@link io.awspring.cloud.sqs.listener.AsyncMessageListener} and set to a
 * {@link io.awspring.cloud.sqs.listener.MessageListenerContainer}.
 * <p>
 * Each method will be handled by a different container instance, created by the specified {@link #factory()} property.
 * If not specified, a default factory will be looked up in the context.
 * <p>
 * When used in conjunction with Spring Boot and auto configuration, the framework supplies a default
 * {@link io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory} and a
 * {@link software.amazon.awssdk.services.sqs.SqsAsyncClient}, unless such beans are already found in the
 * {@link org.springframework.context.ApplicationContext}.
 * <p>
 * For more complex configurations, {@link io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory} instances
 * can be created and configured. See {@link SqsMessageListenerContainerFactory#builder()} for more information on
 * creating and configuring a factory.
 * <p>
 * Further configuration for containers created using this annotation can be achieved by declaring
 * {@link SqsListenerConfigurer} beans.
 * <p>
 * Methods with this annotation can have flexible signatures, including arguments of the following types:
 * <ul>
 * <li>{@link org.springframework.messaging.handler.annotation.Header}</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Headers}</li>
 * <li>{@link org.springframework.messaging.Message}</li>
 * <li>{@link io.awspring.cloud.sqs.listener.Visibility}</li>
 * <li>{@link io.awspring.cloud.sqs.listener.QueueAttributes}</li>
 * <li>{@link software.amazon.awssdk.services.sqs.model.Message}</li>
 * <li>{@link io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement}</li>
 * <li>{@link io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement}</li>
 * </ul>
 * Method signatures also accept {@link java.util.List}&lt;Pojo&gt; and
 * {@link java.util.List}{@link org.springframework.messaging.Message}&lt;Pojo&gt; arguments . Such arguments will
 * configure the container to batch mode. When using List arguments, no other arguments can be provided. Metadata can be
 * retrieved by inspecting the {@link org.springframework.messaging.Message} instances'
 * {@link org.springframework.messaging.MessageHeaders}.
 * <p>
 * To support {@link io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement} and
 * {@link io.awspring.cloud.sqs.listener.acknowledgement.BatchAcknowledgement} arguments, the factory used to create the
 * containers must be set to {@link io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode#MANUAL}.
 * <p>
 * Properties in this annotation support property placeholders ("${...}") and SpEL ("#{...}").
 *
 * @see SqsMessageListenerContainerFactory
 * @see io.awspring.cloud.sqs.listener.SqsMessageListenerContainer
 * @see SqsListenerConfigurer
 *
 * @author Alain Sahli
 * @author Matej Nedic
 * @author Tomaz Fernandes
 * @author Joao Calassio
 * @since 1.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsListener {

	/**
	 * Array of queue names or urls. Queues declared in the same annotation will be handled by the same
	 * {@link io.awspring.cloud.sqs.listener.MessageListenerContainer}.
	 * @return list of queue names or urls.
	 */
	String[] value() default {};

	/**
	 * Alias for {@link #value()}
	 * @return list of queue names or urls.
	 */
	@AliasFor("value")
	String[] queueNames() default {};

	/**
	 * The {@link io.awspring.cloud.sqs.config.MessageListenerContainerFactory} bean name to be used to process this
	 * endpoint.
	 * @return the factory bean name.
	 */
	String factory() default "";

	/**
	 * An id for the {@link io.awspring.cloud.sqs.listener.MessageListenerContainer} that will be created to handle this
	 * endpoint. If none provided a default ID will be created.
	 * @return the container id.
	 */
	String id() default "";

	/**
	 * The maximum concurrent messages that can be processed simultaneously for each queue. Note that if acknowledgement
	 * batching is being used, the actual maximum number of inflight messages might be higher. Default is 10.
	 * @return the maximum number of concurrent messages.
	 */
	String maxConcurrentMessages() default "";

	/**
	 * The maximum number of seconds to wait for messages in a poll to SQS.
	 * @return the poll timeout.
	 */
	String pollTimeoutSeconds() default "";

	/**
	 * The maximum number of messages to poll from SQS. If a value greater than 10 is provided, the result of
	 * multiple polls will be combined, which can be useful for
	 * {@link io.awspring.cloud.sqs.listener.ListenerMode#BATCH}
	 * @return the maximum messages per poll.
	 */
	String maxMessagesPerPoll() default "";

	/**
	 * The message visibility to be applied to messages received from the provided queues. For Standard SQS queues and
	 * batch listeners, visibility will be applied at polling. For single message FIFO queues, visibility is changed
	 * before each remaining message from the same message group is processed.
	 */
	String messageVisibilitySeconds() default "";

	/**
	 * The acknowledgement mode to be used for the provided queues. If not specified, the acknowledgement mode defined
	 * for the container factory will be used.
	 */
	String acknowledgementMode() default "";

}
