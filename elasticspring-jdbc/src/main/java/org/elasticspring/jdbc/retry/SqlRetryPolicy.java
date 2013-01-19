/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.retry;


import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.sql.SQLTransientException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SqlRetryPolicy extends SimpleRetryPolicy {

	public SqlRetryPolicy() {
		super(DEFAULT_MAX_ATTEMPTS, getSqlRetryAbleExceptions());
	}

	private static Map<Class<? extends Throwable>, Boolean> getSqlRetryAbleExceptions() {
		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		retryableExceptions.put(SQLTransientException.class, true);
		retryableExceptions.put(TransientDataAccessException.class, true);
		return retryableExceptions;
	}
}
