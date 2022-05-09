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
package io.awspring.cloud.sqs.listener.sink;

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;

/**
 * {@link MessageSink} extension that uses an {@link AsyncMessageListener} as the output.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessageProcessingPipelineSink<T> extends MessageSink<T>, SmartLifecycle {

	void setMessagePipeline(MessageProcessingPipeline<T> messageProcessingPipeline);

}
