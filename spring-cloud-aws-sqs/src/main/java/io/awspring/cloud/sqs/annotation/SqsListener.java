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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/**
 *
 *
 * @author Alain Sahli
 * @author Matej Nedic
 * @author Tomaz Fernandes
 * @since 1.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqsListener {

	/**
	 * List of queue names. The returned queues will be handled by the same
	 * {@link io.awspring.cloud.sqs.listener.MessageListenerContainer};
	 * @return list of queues
	 */
	String[] value() default {};

	@AliasFor("value")
	String[] queueNames() default {};

	/**
	 * The {@link io.awspring.cloud.sqs.config.MessageListenerContainerFactory} bean name to be used to process this
	 * endpoint.
	 * @return the factory bean name.
	 */
	String factory() default "";

	/**
	 * An ID for the {@link io.awspring.cloud.sqs.listener.MessageListenerContainer} that will be created to handle this
	 * endpoint. If none provided a default ID will be used.
	 * @return the container id.
	 */
	String id() default "";

	/**
	 * The maximum number of inflight messages from each queue in that this endpoint should process simultaneously.
	 * @return the maximum number of inflight messages.
	 */
	String maxInflightMessagesPerQueue() default "";

	/**
	 * The maximum number of seconds to wait for messages in a given poll.
	 * @return the poll timeout.
	 */
	String pollTimeoutSeconds() default "";

	/**
	 */
	String messageVisibilitySeconds() default "";

}
