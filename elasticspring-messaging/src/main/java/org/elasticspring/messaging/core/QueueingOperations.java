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
 *
 */
public interface QueueingOperations {

	void convertAndSend(Object payLoad);

	void convertAndSend(String destinationName, Object payLoad);

	Object receiveAndConvert();

	<T> T receiveAndConvert(Class<T> expectedType);

	Object receiveAndConvert(String destinationName);

	<T> T receiveAndConvert(String destinationName, Class<T> expectedType);
}