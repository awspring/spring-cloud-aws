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
package io.awspring.cloud.sqs.listener;

/**
 * Configure the delivery strategy to be used by a {@link MessageListenerContainer}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.sink.FanOutMessageSink
 * @see io.awspring.cloud.sqs.listener.sink.OrderedMessageSink
 * @see io.awspring.cloud.sqs.listener.sink.BatchMessageSink
 */
public enum MessageDeliveryStrategy {

	/**
	 * Configure the container to receive one message at a time in its components.
	 */
	SINGLE_MESSAGE,

	/**
	 * Configure the container to receive the whole batch of messages in its components.
	 */
	BATCH

}
