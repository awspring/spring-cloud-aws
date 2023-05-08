/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter;

import org.springframework.messaging.MessageHeaders;

/**
 * Mapper interface to map from and to {@link MessageHeaders} and a given source type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <S> the source message type.
 */
public interface HeaderMapper<S> {

	/**
	 * Map the provided {@link MessageHeaders} into the returning message type.
	 * @param headers the headers from which to map.
	 * @return the mapped message instance.
	 */
	S fromHeaders(MessageHeaders headers);

	/**
	 * Map the provided source into a {@link MessageHeaders} instance.
	 * @param source the source from which to map.
	 * @return the mapped {@link MessageHeaders} instance.
	 */
	MessageHeaders toHeaders(S source);

}
