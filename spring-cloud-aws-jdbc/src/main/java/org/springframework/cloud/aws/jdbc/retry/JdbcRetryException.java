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

package org.springframework.cloud.aws.jdbc.retry;

import org.springframework.retry.RetryException;

/**
 * Exception that occurs if a retry operation is installed inside a database transaction.
 * In order to do a successful retry, the database transaction must be closed (rollback)
 * and a new transaction started. Hence it is not possible to do a retry inside a running
 * transactional method.
 *
 * @author Agim Emruli
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class JdbcRetryException extends RetryException {

	JdbcRetryException(String msg) {
		super(msg);
	}

}
