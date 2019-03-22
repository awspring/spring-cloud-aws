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

package org.springframework.cloud.aws.messaging.listener;

import java.util.concurrent.Future;

/**
 * Visibility interface that can be injected as parameter into a listener method. The
 * purpose of this interface is to provide a way for the listener methods to extend the
 * visibility timeout of the message being currently processed.
 *
 * @author Szymon Dembek
 * @since 1.3
 */
public interface Visibility {

	/**
	 * Allows extending the visibility timeout of a message that was already fetched from
	 * the queue, in case when the configured visibility timeout turns out to be to short.
	 * @param seconds number of seconds to extend the visibility timeout by
	 * @return a {@link Future} as the extension can involve some asynchronous request
	 * (i.e. request to an AWS API).
	 */
	Future<?> extend(int seconds);

}
