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
package io.awspring.cloud.sns.core.batch.converter;

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;

/**
 *  Converter interface for converting Spring {@link Message} objects to SNS {@link PublishBatchRequestEntry}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public interface SnsMessageConverter {

	/**
	 * Converts a Spring message to an SNS batch request entry.
	 *
	 * @param message The Spring message to convert
	 * @param <T> The type of the message payload
	 * @return PublishBatchRequestEntry ready to be included in a batch publish request
	 */
	<T> PublishBatchRequestEntry convertMessage(Message<T> message);

}
