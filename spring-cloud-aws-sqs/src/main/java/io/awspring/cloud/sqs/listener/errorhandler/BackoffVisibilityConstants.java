/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.listener.Visibility;

/**
 * Constants for Backoff Error Handler.
 *
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
public class BackoffVisibilityConstants {
	/**
	 * The default initial visibility timeout.
	 */
	static final int DEFAULT_INITIAL_VISIBILITY_TIMEOUT_SECONDS = 100;

	/**
	 * The default multiplier, which doubles the visibility timeout.
	 */
	static final double DEFAULT_MULTIPLIER = 2.0;
	/**
	 * The default increment used by {@link LinearBackoffErrorHandler}.
	 */
	static final int DEFAULT_INCREMENT = 2;

	static final int DEFAULT_MAX_VISIBILITY_TIMEOUT_SECONDS = Visibility.MAX_VISIBILITY_TIMEOUT_SECONDS;
}
