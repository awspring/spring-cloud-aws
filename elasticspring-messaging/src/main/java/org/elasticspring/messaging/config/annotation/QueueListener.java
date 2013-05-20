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

package org.elasticspring.messaging.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to configure listeners for messaging queues (typically an Amazon AWS Simple Queueing Service). This
 * annotation is a method level annotation that allows any arbitrary method to act as a queue listener method.
 * Therefore this annotation allows passive methods to be turned into active listeners that poll the messaging service
 * to receive messages once they arrive.
 * <p/>
 * The requirements on a method are that they receive exactly one parameter that contains the payload of the message.
 * The payload might be a String or any other java object that is created by the {@link
 * org.elasticspring.messaging.support.converter.MessageConverter} from the physical message itself.
 * <p/>
 * The simplest method might look like this
 * <pre>
 * {@code
 * 	class ListenerClass {
 * 		public void listenerMethod(String payload) {
 * 		}
 * 	}
 * }
 * </pre>
 * <p/>
 * A class might contain multiple message listener methods which are listening to different queues. The queue name is
 * directly configured with the annotation itself. Queue names will be resolved with a {@link
 * org.elasticspring.messaging.support.destination.DestinationResolver} which will fetch the physical queue url based
 * on the logical queue name.
 *
 * @author Agim Emruli
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface QueueListener {

	/**
	 * The definition of queue name for the particular message listener method. This value will be resolved through an
	 * {@link org.elasticspring.messaging.support.destination.DestinationResolver} at initialization time to fetch the
	 * physical queue url.
	 * <p/>
	 * <b>Hint:</b>It is also possible to specify a queue url directly which contains the full url to the queue. It is not
	 * recommended to do so as the queue url contains also region specific information.
	 *
	 * @return - the queue name used for this method. Must not be null and not empty if {@link #value()} is not specified.
	 */
	String queueName() default "";

	/**
	 * A shortcut notation to allow more precise annotation like {@code @QueueListener("myName")}. This settings has the
	 * same semantics like {@link #queueName()}
	 *
	 * @return - the queue name used for this method. Must not be null and not empty if {@link #queueName()} is not
	 *         specified
	 */
	String value() default "";

	/**
	 * An optional reference to a {@link org.elasticspring.messaging.support.converter.MessageConverter} used for the
	 * queue
	 * listener method to convert message payload into method parameters. This value of this annotation is actually a bean
	 * name inside the application context to allow MessageConverter instances to be instantiated and configured inside
	 * the
	 * application context.
	 *
	 * @return - the bean name to an {@link org.elasticspring.messaging.support.converter.MessageConverter} instance
	 */
	String messageConverter() default "";
}
