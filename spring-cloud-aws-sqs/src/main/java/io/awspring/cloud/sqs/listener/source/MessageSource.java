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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.listener.ConfigurableContainerComponent;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import org.springframework.messaging.Message;

/**
 * A source of {@link Message} instances. Such messages will be sent for emission to a connected {@link MessageSink}.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessageSource<T> extends ConfigurableContainerComponent {

	/**
	 * Set the {@link MessageSink} to be used as an output for this {@link MessageSource}.
	 * @param messageSink the message sink.
	 */
	void setMessageSink(MessageSink<T> messageSink);

}
